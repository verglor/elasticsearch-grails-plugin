import org.apache.commons.io.FileUtils

eventAllTestsStart = { msg ->
    File pluginsDir = new File('test/resources/plugins')

    Map<String, String> elasticsearchPlugins = [
            'elasticsearch-mapper-attachments' : '3.1.2'
    ]

    if (!pluginsDir.exists()) {

        println "Installing Elasticsearch Plugins ..."

        pluginsDir.mkdirs()

        elasticsearchPlugins.each { pluginName, pluginVersion ->

            String versionedPluginName = "${pluginName}-${pluginVersion}"
            String artifactURL = "http://search.maven.org/remotecontent?filepath=org/elasticsearch/${pluginName}/${pluginVersion}/${versionedPluginName}.zip"

            //Download handling redirects
            File tmpZip = new File(pluginsDir, "${versionedPluginName}.zip")
            while( artifactURL ) {
                artifactURL.toURL().openConnection().with { conn ->
                    conn.instanceFollowRedirects = false
                    artifactURL = conn.getHeaderField( "Location" )
                    if( !artifactURL ) {
                        tmpZip.withOutputStream { out ->
                            conn.inputStream.with { inp ->
                                out << inp
                                inp.close()
                            }
                        }
                    }
                }
            }

            //Explode the plugin
            File explodedPlugin = new File(pluginsDir, versionedPluginName)
            explodedPlugin.mkdirs()
            new AntBuilder().unzip(src: tmpZip.path, dest: explodedPlugin.path, overwrite: false)

            //Delete downloaded file
            tmpZip.delete()
            println "Elasticsearch Plugin : ${versionedPluginName} INSTALLED!"
        }


    }
}

eventTestCaseStart = { name ->
    println '-' * 60
    println "| $name : started"
}

eventTestCaseEnd = { name, err, out ->
    println "\n| $name : finished"
}

eventAllTestsEnd = {msg ->
    def dataFolder = new File('data')
    if (dataFolder.isDirectory()) {
        println "Cleaning up ElasticSerch data directory"
        FileUtils.deleteDirectory(dataFolder)
    }
    println "MC - HERE END!!!"
}
