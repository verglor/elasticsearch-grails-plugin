elasticSearch {
    /**
     * Date formats used by the unmarshaller of the JSON responses
     */
    date.formats = ["yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"]

    /**
     * Hosts for remote ElasticSearch instances.
     * Will only be used with the "transport" client mode.
     * If the client mode is set to "transport" and no hosts are defined, ["localhost", 9300] will be used by default.
     */
    client.hosts = [
            [host: 'localhost', port: 9300]
    ]

    disableAutoIndex = false
    index {
        compound_format = true
    }
    unmarshallComponents = true

    searchableProperty.name = 'searchable'

    includeTransients = false
}

environments {
    development {
        elasticSearch {
            /**
             * Possible values : "local", "node", "transport"
             */
            client.mode = 'local'
            client.transport.sniff = true
            bulkIndexOnStartup = true
        }
    }

    test {
        elasticSearch {
            client.mode = 'local'
            client.transport.sniff = true
            datastoreImpl = 'hibernateDatastore'

            index {
                store.type = 'memory'
                analysis {
                    filter {
                        replace_synonyms {
                            type = 'synonym'
                            synonyms = [
                                    'abc => xyz'
                            ]
                        }
                    }
                    analyzer {
                        test_analyzer {
                            tokenizer = 'standard'
                            filter = [
                                    'lowercase'
                            ]
                        }
                        repl_analyzer {
                            tokenizer = 'standard'
                            filter = [
                                    'lowercase',
                                    'replace_synonyms'
                            ]
                        }
                    }
                }
            }
        }
    }

    production {
        elasticSearch.client.mode = 'node'
    }
}
