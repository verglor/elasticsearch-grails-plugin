package org.grails.plugins.elasticsearch

import grails.test.spock.IntegrationSpec
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.sort.SortBuilder
import org.elasticsearch.search.sort.SortBuilders
import org.elasticsearch.search.sort.SortOrder
import spock.lang.Shared
import test.all.Post

/**
 * @author <a href='mailto:donbeave@gmail.com'>Alexey Zhokhov</a>
 */
class AnalyzersIntegrationSpec extends IntegrationSpec {

    def elasticSearchAdminService
    def elasticSearchService
    @Shared
            posts = []

    def setupSpec() {
        posts << new Post(subject: "[abc] Grails 3.0 M1 Released!",
                body: "Grails 3.0 milestone 1 is now available.").save(failOnError: true)
        posts << new Post(subject: "The Future of Groovy and Grails Sponsorship",
                body: "[abc] http://grails.io/post/108534902333/the-future-of-groovy-grails-sponsorship").save(failOnError: true)
        posts << new Post(subject: "GORM for MongoDB 3.0 Released",
                body: "[xyz] GORM for MongoDB 3.0 has been released with support for MongoDB 2.6 features, including the " +
                        "new GeoJSON types and full text search.").save(failOnError: true)
    }

    def cleanupSpec() {
        posts.each { Post post ->
            post.delete()
        }
    }

    def "search by all"() {
        given:
        elasticSearchAdminService.refresh()
        expect:
        elasticSearchService.search('xyz', [indices: Post, types: Post]).total == 3

        when:
        def results = Post.search('xyz')

        then:
        results.total == 3
    }

    def "search by subject"() {
        given:
        elasticSearchAdminService.refresh()
        expect:
        elasticSearchService.search([indices: Post, types: Post], QueryBuilders.matchQuery('subject', 'xyz')).total == 0

        when:
        def results = Post.search {
            match(subject: 'xyz')
        }

        then:
        results.total == 0
    }

    def "search by body"() {
        given:
        elasticSearchAdminService.refresh()
        expect:
        elasticSearchService.search([indices: Post, types: Post], QueryBuilders.matchQuery('body', 'xyz')).total == 1

        when:
        def results = Post.search {
            match(body: 'xyz')
        }

        then:
        results.total == 1
        results.searchResults[0].body.startsWith('[xyz] GORM')
    }

    def "should sort in descending order of subject"() {
        given:
        elasticSearchAdminService.refresh()

        when:
        SortBuilder sortBuilder = SortBuilders.fieldSort('subject').order(SortOrder.DESC)

        then:
        Post.search('xyz', [indices: Post, types: Post, sort: sortBuilder, from: 0, size: 2]).searchResults*.subject == [
                "The Future of Groovy and Grails Sponsorship",
                "[abc] Grails 3.0 M1 Released!"
        ]

    }

    def "should sort in ascending order of subject"() {
        given:
        elasticSearchAdminService.refresh()

        when:
        SortBuilder sortBuilder = SortBuilders.fieldSort('subject').order(SortOrder.ASC)

        then:
        Post.search('xyz', [indices: Post, types: Post, sort: sortBuilder, from: 0, size: 2]).searchResults*.subject == [
                "[abc] Grails 3.0 M1 Released!",
                "GORM for MongoDB 3.0 Released"
        ]

    }

}
