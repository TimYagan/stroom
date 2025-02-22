package stroom.app.guice;

import com.google.inject.AbstractModule;
import stroom.receive.common.RemoteFeedModule;
import stroom.searchable.impl.SearchableModule;

public class CoreModule extends AbstractModule {
    @Override
    protected void configure() {
        install(new stroom.activity.impl.db.ActivityDbModule());
        install(new stroom.cache.impl.CacheHandlerModule());
        install(new stroom.cache.impl.CacheModule());
        install(new stroom.cluster.lock.impl.db.ClusterLockDbModule());
        install(new stroom.cluster.task.impl.ClusterTaskModule());
        install(new stroom.config.global.impl.db.GlobalConfigDbModule());
        install(new stroom.core.dataprocess.PipelineStreamTaskModule());
        install(new stroom.core.db.DataSourceModule());
        install(new stroom.core.document.DocumentModule());
        install(new stroom.core.entity.cluster.EntityClusterModule());
        install(new stroom.core.entity.event.EntityClusterTaskModule());
        install(new stroom.core.entity.event.EntityEventModule());
        install(new stroom.core.entity.event.EntityEventModule());
        install(new RemoteFeedModule());
        install(new stroom.core.query.QueryModule());
        install(new stroom.core.receive.ReceiveDataModule());
        install(new stroom.core.servlet.ServletModule());
        install(new stroom.core.ui.config.UiConfigModule());
        install(new stroom.core.welcome.BuildInfoModule());
        install(new stroom.core.welcome.WelcomeModule());
        install(new stroom.dashboard.impl.DashboardModule());
        install(new stroom.dashboard.impl.datasource.DatasourceModule());
        install(new stroom.dashboard.impl.logging.LoggingModule());
        install(new stroom.dashboard.impl.script.ScriptModule());
        install(new stroom.dashboard.impl.visualisation.VisualisationModule());
        install(new stroom.data.retention.impl.DataRetentionModule());
        install(new stroom.data.store.impl.DataStoreHandlerModule());
        install(new stroom.data.store.impl.fs.FsDataStoreModule());
        install(new stroom.data.store.impl.fs.db.FsDataStoreDbModule());
        install(new stroom.data.store.impl.fs.FsDataStoreTaskHandlerModule());
        install(new stroom.dictionary.impl.DictionaryHandlerModule());
        install(new stroom.dictionary.impl.DictionaryModule());
        install(new stroom.dictionary.impl.DictionaryTaskHandlerModule());
        install(new stroom.docstore.impl.db.DBPersistenceModule());
        install(new stroom.docstore.impl.DocStoreModule());
//        install(new stroom.elastic.impl.ElasticModule());
//        install(new stroom.elastic.impl.http.HttpElasticModule());
        install(new stroom.event.logging.impl.EventLoggingModule());
        install(new stroom.explorer.impl.db.ExplorerDbModule());
        install(new stroom.explorer.impl.ExplorerModule());
        install(new stroom.feed.impl.FeedModule());
        install(new stroom.importexport.impl.ExportConfigResourceModule());
        install(new stroom.importexport.impl.ImportExportHandlerModule());
        install(new stroom.importexport.impl.ImportExportModule());
        install(new stroom.index.impl.db.IndexDbModule());
        install(new stroom.index.impl.IndexElementModule());
        install(new stroom.index.impl.IndexModule());
        install(new stroom.job.impl.db.JobDbModule());
        install(new stroom.job.impl.JobSystemModule());
        install(new stroom.kafka.impl.KafkaModule());
        install(new stroom.kafka.pipeline.KafkaPipelineModule());
        install(new stroom.meta.impl.db.MetaDbModule());
        install(new stroom.meta.impl.MetaModule());
        install(new stroom.meta.impl.StreamAttributeMapResourceModule());
        install(new stroom.node.impl.db.NodeDbModule());
        install(new stroom.node.impl.NodeModule());
        install(new stroom.node.impl.NodeHandlerModule());
        install(new stroom.node.impl.NodeServiceModule());
        install(new stroom.pipeline.cache.PipelineCacheModule());
        install(new stroom.pipeline.factory.CommonPipelineElementModule());
        install(new stroom.pipeline.factory.DataStorePipelineElementModule());
        install(new stroom.pipeline.factory.PipelineFactoryModule());
        install(new stroom.pipeline.PipelineHandlerModule());
        install(new stroom.pipeline.PipelineModule());
        install(new stroom.pipeline.refdata.ReferenceDataModule());
        install(new stroom.pipeline.stepping.PipelineSteppingModule());
        install(new stroom.pipeline.xsltfunctions.CommonXsltFunctionModule());
        install(new stroom.pipeline.xsltfunctions.DataStoreXsltFunctionModule());
        install(new stroom.processor.impl.db.ProcessorDbModule());
        install(new stroom.processor.impl.ProcessorModule());
        install(new stroom.processor.impl.StreamTaskLifecycleModule());
        install(new stroom.receive.rules.impl.ReceiveDataRulesetModule());
        install(new SearchableModule());
        install(new stroom.search.extraction.ExtractionModule());
        install(new stroom.search.impl.SearchModule());
        install(new stroom.search.impl.shard.ShardModule());
        install(new stroom.search.solr.SolrSearchModule());
        install(new stroom.security.impl.db.SecurityDbModule());
        install(new stroom.security.impl.SecurityModule());
        install(new stroom.servicediscovery.impl.ServiceDiscoveryModule());
        install(new stroom.statistics.impl.hbase.entity.StroomStatsStoreModule());
        install(new stroom.statistics.impl.hbase.internal.InternalModule());
        install(new stroom.statistics.impl.hbase.pipeline.StatisticsElementModule());
        install(new stroom.statistics.impl.hbase.rollup.StroomStatsRollupModule());
        install(new stroom.statistics.impl.InternalStatisticsModule());
        install(new stroom.statistics.impl.sql.entity.StatisticStoreModule());
        install(new stroom.statistics.impl.sql.internal.InternalModule());
        install(new stroom.statistics.impl.sql.rollup.SQLStatisticRollupModule());
        install(new stroom.statistics.impl.sql.search.SQLStatisticSearchModule());
        install(new stroom.statistics.impl.sql.SQLStatisticsModule());
        install(new stroom.storedquery.impl.db.StoredQueryDbModule());
        install(new stroom.storedquery.impl.StoredQueryModule());
        install(new stroom.task.impl.TaskModule());
        install(new stroom.util.pipeline.scope.PipelineScopeModule());
    }
}
