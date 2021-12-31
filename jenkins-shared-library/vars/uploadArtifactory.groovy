#!/usr/bin/env groovy
/*
* Name: Artifactory
* Author: Albert Mugisha
* Date: 02-21-2018
*/

def call(params) {
    def buildServices = [:]
    def services = params.services
    def parallelize = params.parallelize ?: false
    def exclusions = params.exclusions ?: []
    
    for( int z=0; z<services.size(); z++) {
        def service = services[z]
        def serviceStep = "${service}"
        if ( !(service in exclusions)) {
            steps.echo "Artifactory Upload for ${service}"
            if ( parallelize == true) {
                buildServices[serviceStep] = { ->
                    dir("${service}") {
                        uploadArtifactory("${service}")
                    }
                }
            } else {
                dir("${service}") {
                    uploadArtifactory("${service}")
                }
            }
        }
    }

    if( parallelize == true ) {
        steps.parallel buildServices
    }
}


def uploadArtifactory(framework) {
  echo "Currently being re-worked"
  // def ARTIFACT = sh(script:"ls target/*.jar",returnStdout: true).trim()
  // def VERSION = ARTIFACT.split("/")[ARTIFACT.split("/").length -1].split("-")[-2]
  // rtUpload (
  //   serverId: "mop-upload",
  //   spec:
  //   """{
  //     "files": [
  //     {
  //       "pattern": "target/*jar",
  //       "target": "orchestration-snapshots-local/io/netfoundry/${framework}/${VERSION}-SNAPSHOT/"
  //     }
  //     ]
  //     }"""
  //     )
  // rtUpload (
  //   serverId: "mop-upload",
  //   spec:
  //   """{
  //     "files": [
  //     {
  //       "pattern": "pom.xml",
  //       "target": "orchestration-snapshots-local/io/netfoundry/${framework}/${VERSION}-SNAPSHOT/"
  //     }
  //     ]
  //     }"""
  //     )
    }