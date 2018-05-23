/*
 * Copyright 2017 Crown Copyright
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

package stroom.index;

import stroom.docref.DocRef;
import stroom.index.shared.FindIndexShardCriteria;
import stroom.index.shared.IndexDoc;
import stroom.index.shared.IndexShard;
import stroom.index.shared.IndexShard.IndexShardStatus;
import stroom.jobsystem.JobTrackedSchedule;
import stroom.node.NodeCache;
import stroom.node.shared.Node;
import stroom.security.Security;
import stroom.security.shared.PermissionNames;
import stroom.task.GenericServerTask;
import stroom.task.TaskManager;
import stroom.util.io.FileUtil;
import stroom.util.lifecycle.StroomFrequencySchedule;
import stroom.util.lifecycle.StroomSimpleCronSchedule;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;
import stroom.util.logging.LogExecutionTime;
import stroom.util.shared.ModelStringUtil;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;

/**
 * Pool API into open index shards.
 */
@Singleton
public class IndexShardManagerImpl implements IndexShardManager {
    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(IndexShardManagerImpl.class);

    private final IndexStore indexStore;
    private final IndexShardService indexShardService;
    private final Provider<IndexShardWriterCache> indexShardWriterCacheProvider;
    private final NodeCache nodeCache;
    private final TaskManager taskManager;
    private final Security security;

    private final StripedLock shardUpdateLocks = new StripedLock();
    private final AtomicBoolean deletingShards = new AtomicBoolean();

    private final Map<IndexShardStatus, Set<IndexShardStatus>> allowedStateTransitions = new HashMap<>();

    @Inject
    IndexShardManagerImpl(final IndexStore indexStore,
                          final IndexShardService indexShardService,
                          final Provider<IndexShardWriterCache> indexShardWriterCacheProvider,
                          final NodeCache nodeCache,
                          final TaskManager taskManager,
                          final Security security) {
        this.indexStore = indexStore;
        this.indexShardService = indexShardService;
        this.indexShardWriterCacheProvider = indexShardWriterCacheProvider;
        this.nodeCache = nodeCache;
        this.taskManager = taskManager;
        this.security = security;

        allowedStateTransitions.put(IndexShardStatus.CLOSED, new HashSet<>(Arrays.asList(IndexShardStatus.OPEN, IndexShardStatus.DELETED, IndexShardStatus.CORRUPT)));
        allowedStateTransitions.put(IndexShardStatus.OPEN, new HashSet<>(Arrays.asList(IndexShardStatus.CLOSED, IndexShardStatus.DELETED, IndexShardStatus.CORRUPT)));
        allowedStateTransitions.put(IndexShardStatus.DELETED, Collections.emptySet());
        allowedStateTransitions.put(IndexShardStatus.CORRUPT, Collections.singleton(IndexShardStatus.DELETED));
    }

    /**
     * Delete anything that has been marked to delete
     */
    @StroomSimpleCronSchedule(cron = "0 0 *")
    @JobTrackedSchedule(jobName = "Index Shard Delete", description = "Job to delete index shards from disk that have been marked as deleted")
    @Override
    public void deleteFromDisk() {
        security.secure(PermissionNames.MANAGE_INDEX_SHARDS_PERMISSION, () -> {
            if (deletingShards.compareAndSet(false, true)) {
                try {
                    final IndexShardWriterCache indexShardWriterCache = indexShardWriterCacheProvider.get();

                    final FindIndexShardCriteria criteria = new FindIndexShardCriteria();
                    criteria.getNodeIdSet().add(nodeCache.getDefaultNode());
                    criteria.getFetchSet().add(IndexDoc.DOCUMENT_TYPE);
                    criteria.getFetchSet().add(Node.ENTITY_TYPE);
                    criteria.getIndexShardStatusSet().add(IndexShardStatus.DELETED);
                    final List<IndexShard> shards = indexShardService.find(criteria);

                    final GenericServerTask task = GenericServerTask.create("Delete Logically Deleted Shards", "Deleting Logically Deleted Shards...");
                    final Runnable runnable = () -> {
                        try {
                            final LogExecutionTime logExecutionTime = new LogExecutionTime();
                            final Iterator<IndexShard> iter = shards.iterator();
                            while (!Thread.currentThread().isInterrupted() && iter.hasNext()) {
                                final IndexShard shard = iter.next();
                                final IndexShardWriter writer = indexShardWriterCache.getWriterByShardId(shard.getId());
                                try {
                                    if (writer != null) {
                                        LOGGER.debug(() -> "deleteLogicallyDeleted() - Unable to delete index shard " + shard.getId() + " as it is currently in use");
                                    } else {
                                        deleteFromDisk(shard);
                                    }
                                } catch (final RuntimeException e) {
                                    LOGGER.error(e::getMessage, e);
                                }
                            }
                            LOGGER.debug(() -> "deleteLogicallyDeleted() - Completed in " + logExecutionTime);
                        } finally {
                            deletingShards.set(false);
                        }
                    };

                    // In tests we don't have a task manager.
                    if (taskManager == null) {
                        runnable.run();
                    } else {
                        task.setRunnable(runnable);
                        taskManager.execAsync(task);
                    }

                } catch (final RuntimeException e) {
                    LOGGER.error(e::getMessage, e);
                    deletingShards.set(false);
                }
            }
        });
    }

    private void deleteFromDisk(final IndexShard shard) {
        try {
            // Find the index shard dir.
            final Path dir = IndexShardUtil.getIndexPath(shard);

            // See if there are any files in the directory.
            if (!Files.isDirectory(dir) || FileUtil.deleteDir(dir)) {
                // The directory either doesn't exist or we have
                // successfully deleted it so delete this index
                // shard from the database.
                if (indexShardService != null) {
                    indexShardService.delete(shard);
                }
            }
        } catch (final RuntimeException e) {
            LOGGER.error(e::getMessage, e);
        }
    }

    /**
     * This is called by the node command service and is a result of a user
     * interaction. The map is synchronised so no writers will be created or
     * destroyed while this is called.
     */
    @Override
    public Long findFlush(final FindIndexShardCriteria criteria) {
        return security.secureResult(PermissionNames.MANAGE_INDEX_SHARDS_PERMISSION, () -> performAction(criteria, IndexShardAction.FLUSH));
    }

//    /**
//     * This is called when a user wants to close some index shards or during shutdown.
//     * This method returns the number of index shard writers that have been closed.
//     */
//    @Override
//    public Long findClose(final FindIndexShardCriteria criteria) {
//        return performAction(criteria, IndexShardAction.CLOSE);
//    }

    /**
     * This is called by the node command service and is a result of a user
     * interaction. The map is synchronised so no writers will be created or
     * destroyed while this is called.
     */
    @Override
    public Long findDelete(final FindIndexShardCriteria criteria) {
        return security.secureResult(PermissionNames.MANAGE_INDEX_SHARDS_PERMISSION, () -> performAction(criteria, IndexShardAction.DELETE));
    }

    private Long performAction(final FindIndexShardCriteria criteria, final IndexShardAction action) {
        final List<IndexShard> shards = indexShardService.find(criteria);
        return performAction(shards, action);
    }

    private long performAction(final List<IndexShard> shards, final IndexShardAction action) {
        final AtomicLong shardCount = new AtomicLong();

        if (shards.size() > 0) {
            final IndexShardWriterCache indexShardWriterCache = indexShardWriterCacheProvider.get();

            // Create an atomic integer to count the number of index shard writers yet to complete the specified action.
            final AtomicInteger remaining = new AtomicInteger(shards.size());

            // Create a scheduled executor for us to continually log index shard writer action progress.
            final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
            // Start logging action progress.
            executor.scheduleAtFixedRate(() -> LOGGER.info(() -> "Waiting for " + remaining.get() + " index shards to " + action.getName()), 10, 10, TimeUnit.SECONDS);

            // Perform action on all of the index shard writers in parallel.
            shards.parallelStream().forEach(shard -> {
                try {
                    switch (action) {
                        case FLUSH:
                            shardCount.incrementAndGet();
                            indexShardWriterCache.flush(shard.getId());
                            break;
//                                case CLOSE:
//                                    indexShardWriter.close();
//                                    break;
                        case DELETE:
                            shardCount.incrementAndGet();
                            indexShardWriterCache.delete(shard.getId());
                            break;
                    }
                } catch (final RuntimeException e) {
                    LOGGER.error(e::getMessage, e);
                }

                remaining.getAndDecrement();
            });

            // Shut down the progress logging executor.
            executor.shutdown();

            LOGGER.info(() -> "Finished " + action.getActivity() + " index shards");
        }

        return shardCount.get();
    }

    @StroomFrequencySchedule("10m")
    @JobTrackedSchedule(jobName = "Index Shard Retention", description = "Job to set index shards to have a status of deleted that have past their retention period")
    @Override
    public void checkRetention() {
        security.secure(PermissionNames.MANAGE_INDEX_SHARDS_PERMISSION, () -> {
            final FindIndexShardCriteria criteria = new FindIndexShardCriteria();
            criteria.getNodeIdSet().add(nodeCache.getDefaultNode());
            criteria.getFetchSet().add(IndexDoc.DOCUMENT_TYPE);
            criteria.getFetchSet().add(Node.ENTITY_TYPE);
            final List<IndexShard> shards = indexShardService.find(criteria);
            for (final IndexShard shard : shards) {
                checkRetention(shard);
            }
        });
    }

    private void checkRetention(final IndexShard shard) {
        try {
            // Delete this shard if it is older than the retention age.
            final IndexDoc index = indexStore.readDocument(new DocRef(IndexDoc.DOCUMENT_TYPE, shard.getIndexUuid()));
            if (index == null) {
                // If there is no associated index then delete the shard.
                setStatus(shard.getId(), IndexShardStatus.DELETED);

            } else {
                final Integer retentionDayAge = index.getRetentionDayAge();
                final Long partitionToTime = shard.getPartitionToTime();
                if (retentionDayAge != null && partitionToTime != null && !IndexShardStatus.DELETED.equals(shard.getStatus())) {
                    // See if this index shard is older than the index retention
                    // period.
                    final long retentionTime = ZonedDateTime.now(ZoneOffset.UTC).minusDays(retentionDayAge).toInstant().toEpochMilli();

                    if (partitionToTime < retentionTime) {
                        setStatus(shard.getId(), IndexShardStatus.DELETED);
                    }
                }
            }
        } catch (final RuntimeException e) {
            LOGGER.error(e::getMessage, e);
        }
    }

    @Override
    public IndexShard load(final long indexShardId) {
        return security.secureResult(PermissionNames.MANAGE_INDEX_SHARDS_PERMISSION, () -> {
            // Allow the thing to run without a service (e.g. benchmark mode)
            if (indexShardService != null) {
                final Lock lock = shardUpdateLocks.getLockForKey(indexShardId);
                lock.lock();
                try {
                    return indexShardService.loadById(indexShardId);
                } finally {
                    lock.unlock();
                }
            }

            return null;
        });
    }

    @Override
    public void setStatus(final long indexShardId, final IndexShardStatus status) {
        // Allow the thing to run without a service (e.g. benchmark mode)
        if (indexShardService != null) {
            final Lock lock = shardUpdateLocks.getLockForKey(indexShardId);
            lock.lock();
            try {
                final IndexShard indexShard = indexShardService.loadById(indexShardId);
                if (indexShard != null) {
                    // Only allow certain state transitions.
                    final Set<IndexShardStatus> allowed = allowedStateTransitions.get(indexShard.getStatus());
                    if (allowed.contains(status)) {
                        indexShard.setStatus(status);
                        indexShardService.save(indexShard);
                    }
                }
            } catch (final RuntimeException e) {
                LOGGER.error(e::getMessage, e);
            } finally {
                lock.unlock();
            }
        }
    }

    @Override
    public void update(final long indexShardId, final Integer documentCount, final Long commitDurationMs, final Long commitMs, final Long fileSize) {
        // Allow the thing to run without a service (e.g. benchmark mode)
        if (indexShardService != null) {
            final Lock lock = shardUpdateLocks.getLockForKey(indexShardId);
            lock.lock();
            try {
                final IndexShard indexShard = indexShardService.loadById(indexShardId);

                if (documentCount != null) {
                    indexShard.setDocumentCount(documentCount);
                    indexShard.setCommitDocumentCount(documentCount - indexShard.getDocumentCount());

                    // Output some debug so we know how long commits are taking.
                    LOGGER.debug(() -> {
                        final String durationString = ModelStringUtil.formatDurationString(commitDurationMs);
                        return "Documents written since last update " + (documentCount - indexShard.getDocumentCount()) + " ("
                                + durationString + ")";
                    });
                }
                if (commitDurationMs != null) {
                    indexShard.setCommitDurationMs(commitDurationMs);
                }
                if (commitMs != null) {
                    indexShard.setCommitMs(commitMs);
                }
                if (fileSize != null) {
                    indexShard.setFileSize(fileSize);
                }

                indexShardService.save(indexShard);
            } finally {
                lock.unlock();
            }
        }
    }

    private enum IndexShardAction {
        FLUSH("flush", "flushing"), DELETE("delete", "deleting");

        private final String name;
        private final String activity;

        IndexShardAction(final String name, final String activity) {
            this.name = name;
            this.activity = activity;
        }

        public String getName() {
            return name;
        }

        public String getActivity() {
            return activity;
        }
    }
}
