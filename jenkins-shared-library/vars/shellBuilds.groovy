#!/usr/bin/env groovy
/*
* Name: Maven Builds
* Author: Albert Mugisha
* Date: 02-21-2018
*/

def call(params) {
    def buildServices = [:]
    def services = params.services
    def parallelize = params.parallelize ?: false
    def exclusions = params.exclusions ?: []
    def binding = params.binding ?: [:]
    def directory = params.dir ?: null
    def command = params.command
    
    for( int z=0; z<services.size(); z++) {
        def service = services[z]
        def serviceStep = "${service}"
        def changeDir = (directory == "service") ? service : directory
        if ( !(service in exclusions)) {
            steps.echo "Build for ${service}"
            binding.arrayElement = "${service}"
            def engine = new org.apache.commons.lang3.text.StrSubstitutor(binding)
            def new_command = engine.replace(command)
            engine = null 

            if ( parallelize == true) {
                buildServices[serviceStep] = { ->
                    if ( changeDir == null) {
                        steps.echo "Building ${serviceStep}"
                        steps.sh "${new_command}"
                    } else {
                        dir("${changeDir}") {
                            steps.echo "Building ${serviceStep}"
                            steps.sh "${new_command}"
                        }
                    }
                }
            } else {
                if ( changeDir == null) {
                    steps.echo "Building ${serviceStep}"
                    steps.sh "${new_command}"
                } else {
                    dir("${service}") {
                        steps.echo "Building ${serviceStep}"
                        steps.sh "${new_command}"
                    }
                }
            }
        }
    }

    if( parallelize == true ) {
        steps.parallel buildServices
    }
}