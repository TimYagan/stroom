/*
 * Copyright 2016 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stroom.search.impl.shard;

import stroom.pipeline.errorhandler.ErrorReceiver;
import stroom.search.extraction.Values;
import stroom.search.impl.shard.IndexShardSearchTask.IndexShardQueryFactory;
import stroom.search.impl.shard.IndexShardSearchTask.ResultReceiver;
import stroom.task.shared.ThreadPool;
import stroom.task.shared.ThreadPoolImpl;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;
import stroom.util.shared.Severity;
import stroom.util.task.taskqueue.AbstractTaskProducer;
import stroom.util.task.taskqueue.TaskExecutor;
import stroom.util.task.taskqueue.TaskProducer;

import javax.inject.Provider;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class IndexShardSearchTaskProducer extends AbstractTaskProducer implements TaskProducer {
    private static final LambdaLogger LAMBDA_LOGGER = LambdaLoggerFactory.getLogger(IndexShardSearchTaskProducer.class);

    public static final ThreadPool THREAD_POOL = new ThreadPoolImpl(
            "Search Index Shard",
            5,
            0,
            Integer.MAX_VALUE);

    private final ErrorReceiver errorReceiver;
    private final Queue<IndexShardSearchRunnable> taskQueue = new ConcurrentLinkedQueue<>();
    private final long now = System.currentTimeMillis();
    private final AtomicInteger threadsUsed = new AtomicInteger();

    private final int maxThreadsPerTask;

    private final int tasksTotal;
    private final AtomicInteger tasksRemaining = new AtomicInteger();
    private final AtomicInteger tasksRequested = new AtomicInteger();
    private final CountDownLatch completionLatch = new CountDownLatch(1);
    private final LinkedBlockingQueue<Values> storedData;

    public IndexShardSearchTaskProducer(final TaskExecutor taskExecutor,
                                        final LinkedBlockingQueue<Values> storedData,
                                        final List<Long> shards,
                                        final IndexShardQueryFactory queryFactory,
                                        final String[] fieldNames,
                                        final ErrorReceiver errorReceiver,
                                        final AtomicLong hitCount,
                                        final int maxThreadsPerTask,
                                        final Executor executor,
                                        final Provider<IndexShardSearchTaskHandler> handlerProvider) {
        super(taskExecutor, executor);
        this.maxThreadsPerTask = maxThreadsPerTask;
        this.storedData = storedData;
        this.errorReceiver = errorReceiver;

        // Create a deque to capture stored data from the index that can be used by coprocessors.
        final ResultReceiver resultReceiver = (shardId, values) -> putValues(values);

        for (final Long shard : shards) {
            final IndexShardSearchTask task = new IndexShardSearchTask(queryFactory, shard, fieldNames, resultReceiver, errorReceiver, hitCount);
            final IndexShardSearchRunnable runnable = new IndexShardSearchRunnable(task, handlerProvider);
            taskQueue.add(runnable);
        }
        final int count = taskQueue.size();
        LAMBDA_LOGGER.debug(() -> String.format("Queued %s index shard search tasks", count));

        // Set the total number of index shards we are searching.
        // We set this here so that any errors that might have occurred adding shard search tasks would prevent this producer having work to do.
        tasksTotal = count;
        tasksRemaining.set(tasksTotal);

        // Attach to the supplied executor.
        attach();

        // Tell the supplied executor that we are ready to deliver tasks.
        signalAvailable();
    }

    private void putValues(final Values values) {
        try {
            storedData.put(values);
        } catch (final InterruptedException e) {
            error(e.getMessage(), e);

            // Continue to interrupt this thread.
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Get the next task to execute or null if the producer has reached a concurrent execution limit or no further tasks
     * are available.
     *
     * @return The next task to execute or null if no tasks are available at this time.
     */
    @Override
    public final Runnable next() {
        Runnable runnable = null;

        final int count = threadsUsed.incrementAndGet();
        if (count > maxThreadsPerTask) {
            threadsUsed.decrementAndGet();
        } else {
            final Runnable task = getNext();
            if (task == null) {
                // There are no remaining tasks.
                threadsUsed.decrementAndGet();
                // Auto detach if we are complete.
                detach();
            } else {
                runnable = () -> {
                    try {
                        task.run();
                    } catch (final RuntimeException e) {
                        LAMBDA_LOGGER.debug(e::getMessage, e);
                    } finally {
                        threadsUsed.decrementAndGet();
                        final int remaining = tasksRemaining.decrementAndGet();
                        if (remaining <= 0) {
                            complete();
                        }
                    }
                };
            }
        }

        return runnable;
    }

    @Override
    public int compareTo(final TaskProducer o) {
        return Long.compare(now, ((IndexShardSearchTaskProducer) o).now);
    }

    /**
     * Wait for this task producer to not issue any further tasks and that all of the tasks it has issued have completed processing.
     */
    public void awaitCompletion() throws InterruptedException {
        completionLatch.await();
    }

    private Runnable getNext() {
        final IndexShardSearchRunnable task = taskQueue.poll();
        if (task != null) {
            final int no = tasksRequested.incrementAndGet();
            task.getTask().setShardTotal(tasksTotal);
            task.getTask().setShardNumber(no);
        }

        return task;
    }

    private void complete() {
        taskQueue.clear();
        tasksRemaining.set(0);

        try {
            storedData.put(Values.COMPLETE);
        } catch (final InterruptedException e) {
            LAMBDA_LOGGER.debug(e::getMessage, e);

            // Continue to interrupt this thread.
            Thread.currentThread().interrupt();
        }

        try {
            completionLatch.countDown();
        } finally {
            // Ensure we are detached.
            detach();
        }
    }

    private void error(final String message, final Throwable t) {
        errorReceiver.log(Severity.ERROR, null, null, message, t);
    }

    @Override
    public String toString() {
        final int remaining = tasksRemaining.get();
        return getClass().getSimpleName() +
                " (remaining=" +
                remaining +
                ", completed=" +
                (tasksTotal - remaining) +
                ", total=" +
                tasksTotal +
                ")";
    }

    private static class IndexShardSearchRunnable implements Runnable {
        private final IndexShardSearchTask task;
        private final Provider<IndexShardSearchTaskHandler> handlerProvider;

        IndexShardSearchRunnable(final IndexShardSearchTask task, final Provider<IndexShardSearchTaskHandler> handlerProvider) {
            this.task = task;
            this.handlerProvider = handlerProvider;
        }

        @Override
        public void run() {
            final IndexShardSearchTaskHandler handler = handlerProvider.get();
            handler.exec(task);
        }

        public IndexShardSearchTask getTask() {
            return task;
        }
    }
}
