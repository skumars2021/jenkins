#!/usr/bin/env groovy
/*
* Name: Artifactory
* Author: Albert Mugisha
* Date: 02-21-2018
*/

import groovy.json.JsonSlurper

def call(params) {    
    def REPO = params.REPO
    def BUILDPATH = params.BUILDPATH
    def ARTIFACTORY_REPO = params.ARTIFACTORY_REPO ?: "mop-local-snapshot"
    def VERSION = sh (script: "mvn org.apache.maven.plugins:maven-help-plugin:3.1.0:evaluate -Dexpression=project.version -q -DforceStdout", returnStdout: true).trim()
   
    echo "Artifactory Upload:- ${REPO}: ${VERSION}"
    
    sh "pwd"
    try{
        sh "ls ${BUILDPATH}/io/netfoundry/${REPO}/${VERSION}/*"
    } catch(err) {
        error "Artifact Directory not found. ${err}"
    }

    rtUpload (
        serverId: "mop-user",
        spec:
        """{
        "files": [
        {
            "pattern": "${BUILDPATH}/io/netfoundry/${REPO}/${VERSION}/*jar",
            "target": "${ARTIFACTORY_REPO}/io/netfoundry/${REPO}/${VERSION}/"
        }
        ]
        }"""
        )
    rtUpload (
        serverId: "mop-user",
        spec:
        """{
        "files": [
        {
            "pattern": "${BUILDPATH}/io/netfoundry/${REPO}/${VERSION}/*pom",
            "target": "${ARTIFACTORY_REPO}/io/netfoundry/${REPO}/${VERSION}/"
        }
        ]
        }"""
    )
    
    rtPublishBuildInfo (
        serverId: 'mop-user',

    )
}