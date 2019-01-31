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
 *
 */

package stroom.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.entity.shared.BaseResultList;
import stroom.entity.shared.ResultList;
import stroom.job.api.Job;
import stroom.job.api.JobNode;
import stroom.job.api.JobNodeService;
import stroom.job.shared.FetchJobDataAction;
import stroom.job.shared.FindJobNodeCriteria;
import stroom.job.shared.JobNodeInfo;
import stroom.job.shared.JobNodeRow;
import stroom.node.shared.Node;
import stroom.security.Security;
import stroom.task.api.AbstractTaskHandler;
import stroom.task.cluster.api.ClusterCallEntry;
import stroom.task.cluster.api.ClusterDispatchAsyncHelper;
import stroom.task.cluster.api.DefaultClusterResultCollector;
import stroom.task.cluster.api.TargetType;
import stroom.util.shared.SharedMap;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


class FetchJobDataHandler extends AbstractTaskHandler<FetchJobDataAction, ResultList<JobNodeRow>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(FetchJobDataHandler.class);

    private final JobNodeService jobNodeService;
    private final ClusterDispatchAsyncHelper dispatchHelper;
    private final Security security;

    @Inject
    FetchJobDataHandler(final JobNodeService jobNodeService,
                        final ClusterDispatchAsyncHelper dispatchHelper,
                        final Security security) {
        this.jobNodeService = jobNodeService;
        this.dispatchHelper = dispatchHelper;
        this.security = security;
    }

    @Override
    public BaseResultList<JobNodeRow> exec(final FetchJobDataAction action) {
        return security.secureResult(() -> {
            List<Job> result;
            try {
                result = jobNodeService.find().fetch(activity.getId());
                entityEventLog.view(result, null);
            } catch (final RuntimeException e) {
                entityEventLog.view(activity, e);
                throw e;
            }




            return result;
            // Add the root node.
            final List<JobNodeRow> values = new ArrayList<>();

            if (action.getJob() == null) {
                return BaseResultList.createUnboundedList(values);
            }

            DefaultClusterResultCollector<SharedMap<JobNode, JobNodeInfo>> collector;
            collector = dispatchHelper.execAsync(new JobNodeInfoClusterTask(action.getUserToken()), TargetType.ACTIVE);

            final FindJobNodeCriteria criteria = new FindJobNodeCriteria();
            criteria.getJobIdSet().add(action.getJob());
            criteria.getFetchSet().add(Node.ENTITY_TYPE);
            criteria.getFetchSet().add(Job.ENTITY_TYPE);

            final List<JobNode> jobNodes = jobNodeService.find(criteria);

            // Sort job nodes by node name.
            jobNodes.sort((JobNode o1, JobNode o2) -> o1.getNode().getName().compareToIgnoreCase(o2.getNode().getName()));

            // Create the JobNodeRow value
            for (final JobNode jobNode : jobNodes) {
                JobNodeInfo jobNodeInfo = null;

                final ClusterCallEntry<SharedMap<JobNode, JobNodeInfo>> response = collector.getResponse(jobNode.getNode().getName());

                if (response == null) {
                    LOGGER.debug("No response for: {}", jobNode);
                } else if (response.getError() != null) {
                    LOGGER.debug("Error response for: {} - {}", jobNode, response.getError().getMessage());
                    LOGGER.debug(response.getError().getMessage(), response.getError());
                } else {
                    final Map<JobNode, JobNodeInfo> map = response.getResult();
                    if (map == null) {
                        LOGGER.warn("No data for: {}", jobNode);
                    } else {
                        jobNodeInfo = map.get(jobNode);
                    }
                }

                final JobNodeRow jobNodeRow = new JobNodeRow(jobNode, jobNodeInfo);
                values.add(jobNodeRow);
            }

            return BaseResultList.createUnboundedList(values);
        });
    }
}
