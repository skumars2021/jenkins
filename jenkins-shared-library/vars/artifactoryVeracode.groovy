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
    def ARTIFACTORY_REPO = params.ARTIFACTORY_REPO?: "veracode-jars"
    def ACTION = params.ACTION ?: "download"
   
    

    

    if( ACTION == "upload") {
        def VERSION = sh (script: "mvn org.apache.maven.plugins:maven-help-plugin:3.1.0:evaluate -Dexpression=project.version -q -DforceStdout", returnStdout: true).trim()
        echo "Veracode Artifactory Upload:- ${REPO}: ${VERSION}"
        try{
            sh "ls ${BUILDPATH}/io/netfoundry/${REPO}/${VERSION}/*"
        } catch(err) {
            error "Artifact Directory not found. ${err}"
        }
        def result = rtUpload (
            serverId: "mop-user",
            spec:
            """{
            "files": [
            {
                "pattern": "${BUILDPATH}/io/netfoundry/${REPO}/${VERSION}/*jar",
                "target": "${ARTIFACTORY_REPO}/${REPO}/"
            }
            ]
            }"""
        )
        println "Upload Result is ${result}"
    } else {
        if (ARTIFACTORY_REPO == "nfconsole" || ARTIFACTORY_REPO == "netfoundry-ui"){
            echo "Veracode Artifactory Download:- ${ARTIFACTORY_REPO}/${REPO}"
            def result = rtDownload (
                serverId: "mop-user",
                spec:
                """{
                "files": [
                {
                    "pattern": "${ARTIFACTORY_REPO}/${REPO}",
                    "target": "./"
                }
                ]
                }"""
            )
        }
        else {
            echo "Veracode Artifactory Download:- ${ARTIFACTORY_REPO}/${REPO}/"
            def result = rtDownload (
                serverId: "mop-user",
                spec:
                """{
                "files": [
                {
                    "pattern": "${ARTIFACTORY_REPO}/${REPO}/",
                    "target": "./"
                }
                ]
                }"""
            )
        }
    }
}