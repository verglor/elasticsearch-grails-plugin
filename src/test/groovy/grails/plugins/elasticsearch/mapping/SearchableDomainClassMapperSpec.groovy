package grails.plugins.elasticsearch.mapping

import test.File
import grails.test.mixin.Mock
import org.grails.config.NavigableMap
import org.grails.core.DefaultGrailsDomainClass
import grails.core.GrailsApplication
import spock.lang.Specification
import test.Building
import test.Product

@Mock(Product)
class SearchableDomainClassMapperSpec extends Specification {

    void 'a domain class with mapping geoPoint: true is mapped as a geo_point'() {
        NavigableMap config = [:] as NavigableMap
        GrailsApplication grailsApplication = [:] as GrailsApplication

        given: 'a mapper for Building'

        def clazz = new DefaultGrailsDomainClass(Building)
        SearchableDomainClassMapper mapper = new SearchableDomainClassMapper(grailsApplication, clazz, config)

        when: 'the mapping is built'
        def classMapping = mapper.buildClassMapping()

        then: 'the location is mapped as a geoPoint'
        classMapping.domainClass == clazz
        classMapping.elasticTypeName == 'building'
        def locationMapping = classMapping.propertiesMapping.find { it.propertyName == 'location' }
        locationMapping.isGeoPoint()
    }
    
    void 'a domain class with mapping attachment: true is mapped as attachment'() {
        NavigableMap config = [:] as NavigableMap
        GrailsApplication grailsApplication = [:] as GrailsApplication

        given: 'a mapper for File'

        def clazz = new DefaultGrailsDomainClass(File)
        SearchableDomainClassMapper mapper = new SearchableDomainClassMapper(grailsApplication, clazz, config)

        when: 'the mapping is built'
        def classMapping = mapper.buildClassMapping()

        then: 'the attachment property is mapped as attachment'
        classMapping.domainClass == clazz
        classMapping.elasticTypeName == 'file'
        def locationMapping = classMapping.propertiesMapping.find { it.propertyName == 'attachment' }
        locationMapping.isAttachment()
    }

    void 'the correct mapping is passed to the ES server'() {
        NavigableMap config = new NavigableMap()
        GrailsApplication grailsApplication = [:] as GrailsApplication

        given: 'a mapper for Building'

        def clazz = new DefaultGrailsDomainClass(Building)
        SearchableDomainClassMapper mapper = new SearchableDomainClassMapper(grailsApplication, clazz, config)
        def classMapping = mapper.buildClassMapping()
        def mapping = ElasticSearchMappingFactory.getElasticMapping(classMapping)
        mapping == [
                building: [
                        properties: [
                                location: [
                                        type          : 'geo_point',
                                        include_in_all: true
                                ]
                        ]
                ]
        ]
    }

    void 'a mapping can be built from a domain class'() {
        NavigableMap config = [:] as NavigableMap
        GrailsApplication grailsApplication = [:] as GrailsApplication

        given: 'a mapper for a domain class'
        SearchableDomainClassMapper mapper = new SearchableDomainClassMapper(grailsApplication, new DefaultGrailsDomainClass(Product), config)

        expect: 'a mapping can be built from this domain class'
        mapper.buildClassMapping()
    }

    void 'a mapping is aliased'() {
        NavigableMap config = [:] as NavigableMap
        GrailsApplication grailsApplication = [:] as GrailsApplication

        given: 'a mapper for Building'

        def clazz = new DefaultGrailsDomainClass(Building)
        SearchableDomainClassMapper mapper = new SearchableDomainClassMapper(grailsApplication, clazz, config)

        when: 'the mapping is built'
        def classMapping = mapper.buildClassMapping()

        then: 'the date is aliased'
        classMapping.domainClass == clazz
        classMapping.elasticTypeName == 'building'
        def aliasMapping = classMapping.propertiesMapping.find { it.propertyName == 'date' }
        aliasMapping.isAlias()
    }

    void 'can get the mapped alias'() {
        NavigableMap config = [:] as NavigableMap
        GrailsApplication grailsApplication = [:] as GrailsApplication

        given: 'a mapper for Building'

        def clazz = new DefaultGrailsDomainClass(Building)
        SearchableDomainClassMapper mapper = new SearchableDomainClassMapper(grailsApplication, clazz, config)

        when: 'the mapping is built'
        def classMapping = mapper.buildClassMapping()

        then: 'the date is aliased'
        classMapping.domainClass == clazz
        classMapping.elasticTypeName == 'building'
        def aliasMapping = classMapping.propertiesMapping.find { it.propertyName == 'date' }
        aliasMapping.getAlias() == "@timestamp"
    }
}
