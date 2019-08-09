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

package stroom.explorer;

import org.junit.Assert;
import org.junit.Test;
import stroom.explorer.api.ExplorerService;
import stroom.explorer.impl.ExplorerTreeDao;
import stroom.explorer.impl.ExplorerTreeNode;
import stroom.explorer.shared.ExplorerNode;
import stroom.explorer.shared.ExplorerTreeFilter;
import stroom.explorer.shared.FetchExplorerNodeResult;
import stroom.explorer.shared.FindExplorerNodeCriteria;
import stroom.security.api.Security;
import stroom.security.api.SecurityContext;
import stroom.security.api.UserTokenUtil;
import stroom.security.impl.DocumentPermissionService;
import stroom.security.impl.UserService;
import stroom.security.shared.DocumentPermissionNames;
import stroom.security.shared.User;
import stroom.test.StroomIntegrationTest;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

//@ActiveProfiles(value = {
//        StroomSpringProfiles.PROD,
//        StroomSpringProfiles.IT,
//        SecurityConfiguration.PROD_SECURITY})
//@ContextConfiguration(classes = {
//        DashboardConfiguration.class,
//        EventLoggingConfiguration.class,
//        IndexConfiguration.class,
//        MetaDataStatisticConfiguration.class,
//        PersistenceConfiguration.class,
//        DictionaryConfiguration.class,
//        PipelineConfiguration.class,
//        RuleSetConfiguration.class,
//        ScopeConfiguration.class,
//        ScopeTestConfiguration.class,
//        ScriptConfiguration.class,
//        SearchConfiguration.class,
//        SecurityConfiguration.class,
//        ExplorerConfiguration.class,
//        ServerComponentScanTestConfiguration.class,
//        ServerConfiguration.class,
//        StatisticsConfiguration.class,
//        VisualisationConfiguration.class})
public class TestExplorerTreePerformance extends StroomIntegrationTest {
    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(TestExplorerTreePerformance.class);
    private static final int MAX_CHILDREN = 200;
    private static final int MAX_TREE_DEPTH = 2;

    @Inject
    private ExplorerTreeDao explorerTreeDao;
    @Inject
    private ExplorerService explorerService;
    @Inject
    private UserService userService;
    @Inject
    private DocumentPermissionService documentPermissionService;
    @Inject
    private SecurityContext securityContext;
    @Inject
    private Security security;

    @Test
    public void testLargeTreePerformance() {
        security.asProcessingUser(() -> {
            final FindExplorerNodeCriteria findExplorerNodeCriteria = new FindExplorerNodeCriteria(
                    new HashSet<>(),
                    new HashSet<>(),
                    new ExplorerTreeFilter(
                            Collections.singleton("test"),
                            null,
                            Collections.singleton(DocumentPermissionNames.READ),
                            null,
                            false),
                    Integer.MAX_VALUE,
                    new HashSet<>());

            LOGGER.logDurationIfInfoEnabled(() -> {
                explorerService.clear();
                FetchExplorerNodeResult result = explorerService.getData(findExplorerNodeCriteria);
                Assert.assertNull(result.getTreeStructure().getRoot());
            }, "Checked empty tree");

            final int count = (int) Math.pow(MAX_CHILDREN, MAX_TREE_DEPTH) + MAX_CHILDREN + 1;
            LOGGER.info(() -> "Creating " + count + " tree nodes");
            LOGGER.logDurationIfInfoEnabled(() -> {
                ExplorerTreeNode root = explorerTreeDao.createRoot(newTreePojo("System"));
                addChildren(root, 1, MAX_TREE_DEPTH);
            }, "Created " + count + " tree nodes");

            LOGGER.logDurationIfInfoEnabled(() -> {
                // Check create model.
                explorerTreeDao.createModel(null);
            }, "Create model");

            final AtomicReference<ExplorerNode> lastChild = new AtomicReference<>();
            LOGGER.logDurationIfInfoEnabled(() -> {
                lastChild.set(expandTree(findExplorerNodeCriteria, count));
            }, "Expand all");

            final User user = userService.createUser("testuser");
            final User userGroup = userService.createUserGroup("testusergroup");
            userService.addUserToGroup(user.getUuid(), userGroup.getUuid());
            documentPermissionService.addPermission(lastChild.get().getDocRef().getUuid(), user.getUuid(), DocumentPermissionNames.READ);
            documentPermissionService.addPermission(lastChild.get().getDocRef().getUuid(), userGroup.getUuid(), DocumentPermissionNames.READ);

            LOGGER.logDurationIfInfoEnabled(() -> {
                security.asUser(UserTokenUtil.create(user.getName(), null), () -> {
                    // See what we get back with a user with limited permissions.
                    expandTree(findExplorerNodeCriteria, 3);
                });
            }, "Expand all as user with empty cache");

            LOGGER.logDurationIfInfoEnabled(() -> {
                security.asUser(UserTokenUtil.create(user.getName(), null), () -> {
                    // See what we get back with a user with limited permissions.
                    expandTree(findExplorerNodeCriteria, 3);
                });
            }, "Expand all as user with full cache");
        });
    }

    private ExplorerNode expandTree(final FindExplorerNodeCriteria findExplorerNodeCriteria, final int expected) {
        final AtomicInteger count = new AtomicInteger();
        final AtomicReference<ExplorerNode> lastChild = new AtomicReference<>();

        explorerService.clear();
        FetchExplorerNodeResult result = explorerService.getData(findExplorerNodeCriteria);
        count(result.getTreeStructure().getRoot(), result, count, lastChild);

        Assert.assertEquals(expected, count.get());

        return lastChild.get();
    }

    private void count(final ExplorerNode parent, final FetchExplorerNodeResult result, final AtomicInteger count, final AtomicReference<ExplorerNode> lastChild) {
        if (parent != null) {
            lastChild.set(parent);
            count.incrementAndGet();
            final List<ExplorerNode> children = result.getTreeStructure().getChildren(parent);
            if (children != null) {
                for (final ExplorerNode child : children) {
                    count(child, result, count, lastChild);
                }
            }
        }
    }

    private ExplorerNode openAll(final ExplorerNode parent, final FetchExplorerNodeResult result, final FindExplorerNodeCriteria findExplorerNodeCriteria) {
        ExplorerNode lastChild = null;

        final List<ExplorerNode> children = result.getTreeStructure().getChildren(parent);
        if (children != null && children.size() > 0) {
            findExplorerNodeCriteria.getOpenItems().addAll(children);
            for (final ExplorerNode child : children) {
                lastChild = openAll(child, result, findExplorerNodeCriteria);
                if (lastChild == null) {
                    lastChild = child;
                }
            }
        }

        return lastChild;
    }

    private void addChildren(final ExplorerTreeNode parent, final int depth, final int maxDepth) {
        for (int i = 1; i <= MAX_CHILDREN; i++) {
            final ExplorerTreeNode child = explorerTreeDao.addChild(parent, newTreePojo(parent.getName() + "-" + i));
            if (depth < maxDepth) {
                addChildren(child, depth + 1, maxDepth);
            }
        }
    }

    private ExplorerTreeNode newTreePojo(final String name) {
        final ExplorerTreeNode explorerTreeNode = new ExplorerTreeNode();
        explorerTreeNode.setName(name);
        explorerTreeNode.setType("test");
        explorerTreeNode.setUuid(UUID.randomUUID().toString());
        return explorerTreeNode;
    }
}
