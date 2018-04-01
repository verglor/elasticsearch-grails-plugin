package grails.plugins.elasticsearch

import groovy.transform.CompileStatic
import org.elasticsearch.search.aggregations.Aggregation
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField

@CompileStatic
class ElasticSearchResult {
    Long total
    List searchResults = []
    List<Map<String, HighlightField>> highlight = []
    Map<String, Float> scores = [:]
    Map<String, Object[]> sort = [:]
    Map<String, Aggregation> aggregations = [:]
}
