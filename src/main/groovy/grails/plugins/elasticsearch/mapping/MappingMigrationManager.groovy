package grails.plugins.elasticsearch.mapping

import grails.core.GrailsApplication
import grails.plugins.elasticsearch.ElasticSearchAdminService
import grails.plugins.elasticsearch.ElasticSearchContextHolder
import grails.plugins.elasticsearch.exception.MappingException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static grails.plugins.elasticsearch.mapping.MappingMigrationStrategy.*
import static grails.plugins.elasticsearch.util.IndexNamingUtils.indexingIndexFor
import static grails.plugins.elasticsearch.util.IndexNamingUtils.queryingIndexFor

/**
 * Created by @marcos-carceles on 26/01/15.
 */
class MappingMigrationManager {

    private ElasticSearchContextHolder elasticSearchContextHolder
    private ElasticSearchAdminService es
    private GrailsApplication grailsApplication
    private ConfigObject config

    private static final Logger LOG = LoggerFactory.getLogger(this)

    private Map getEsConfig() {
        grailsApplication.config.elasticSearch as Map
    }

    def applyMigrations(MappingMigrationStrategy migrationStrategy, Map<SearchableClassMapping, Map> elasticMappings, List<MappingConflict> mappingConflicts, Map indexSettings) {
        switch (migrationStrategy) {
            case delete:
                LOG.error("Delete a Mapping is no longer supported since Elasticsearch 2.0 (see https://www.elastic.co/guide/en/elasticsearch/reference/2.0/indices-delete-mapping.html)." +
                        " To prevent data loss, this strategy has been replaced by 'deleteIndex'")
                throw new MappingException()
            case deleteIndex:
                applyDeleteIndexStrategy(elasticMappings, mappingConflicts, indexSettings)
            case alias:
                applyAliasStrategy(elasticMappings, mappingConflicts, indexSettings)
                break;
            case none:
                LOG.error("Could not install mappings : ${mappingConflicts}. No migration strategy selected.")
                throw new MappingException()
        }
    }

    def applyDeleteIndexStrategy(Map<SearchableClassMapping, Map> elasticMappings, List<MappingConflict> mappingConflicts, Map indexSettings) {
        List indices = mappingConflicts.collect { it.scm.indexName } as Set
        indices.each { String indexName ->

            es.deleteIndex indexName

            int nextVersion = es.getNextVersion(indexName)
            boolean buildQueryingAlias = (!!esConfig.bulkIndexOnStartup) && (!esConfig.migration.disableAliasChange)

            rebuildIndexWithMappings(indexName, nextVersion, indexSettings, elasticMappings, buildQueryingAlias)
        }
        indices
    }

    def applyAliasStrategy(Map<SearchableClassMapping, Map> elasticMappings, List<MappingConflict> mappingConflicts, Map indexSettings) {

        List indices = mappingConflicts.collect { it.scm.indexName } as Set

        indices.each { String indexName ->
            LOG.debug("Creating new version and alias for conflicting index ${indexName}")
            boolean conflictOnAlias = es.aliasExists(indexName)
            if (conflictOnAlias || esConfig.migration.aliasReplacesIndex) {

                if (!conflictOnAlias) {
                    es.deleteIndex(indexName)
                }

                int nextVersion = es.getNextVersion(indexName)
                boolean buildQueryingAlias = (!esConfig.bulkIndexOnStartup) && (!conflictOnAlias || !esConfig.migration.disableAliasChange)
                rebuildIndexWithMappings(indexName, nextVersion, indexSettings, elasticMappings, buildQueryingAlias)

            } else {
                throw new MappingException("Could not create alias ${indexName} to solve error installing mappings, index with the same name already exists.")
            }
        }
        indices
    }

    private void rebuildIndexWithMappings(String indexName, int nextVersion, Map indexSettings, Map<SearchableClassMapping, Map> elasticMappings, boolean buildQueryingAlias) {
        Map<String, Map> esMappings = elasticMappings.findAll { SearchableClassMapping scm, Map esMapping ->
            scm.indexName == indexName && scm.isRoot()
        }.collectEntries { SearchableClassMapping scm, Map esMapping ->
            [(scm.elasticTypeName) : esMapping]
        }
        es.createIndex indexName, nextVersion, indexSettings, esMappings
        es.waitForIndex indexName, nextVersion //Ensure it exists so later on mappings are created on the right version
        es.pointAliasTo indexName, indexName, nextVersion
        es.pointAliasTo indexingIndexFor(indexName), indexName, nextVersion
        if (buildQueryingAlias) {
            es.pointAliasTo queryingIndexFor(indexName), indexName, nextVersion
        }
    }

    void setElasticSearchContextHolder(ElasticSearchContextHolder elasticSearchContextHolder) {
        this.elasticSearchContextHolder = elasticSearchContextHolder
    }

    void setGrailsApplication(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication
    }

    void setConfig(ConfigObject config) {
        this.config = config
    }

    void setEs(ElasticSearchAdminService es) {
        this.es = es
    }

}
