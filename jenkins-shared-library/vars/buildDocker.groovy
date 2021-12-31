#!/usr/bin/env groovy
/*
* Name: Artifactory
* Author: Albert Mugisha
* Date: 02-21-2018
*/

def call(params) {    
    def REPO = params.REPO
    def GIT_COMMIT = params.GIT_COMMIT
    def BUILD_NUMBER = params.BUILD_NUMBER
    def BRANCH_NAME = params.BRANCH_NAME
    def NEW_BRNCH_PREFIX = BRANCH_NAME.replace("/","-")
    def VERSION = sh (script: "mvn org.apache.maven.plugins:maven-help-plugin:3.1.0:evaluate -Dexpression=project.version -q -DforceStdout", label: "Get Version of Jar", returnStdout: true).trim()
    VERSION = VERSION.split("-")[0]
    def ARTIFACT = sh(script:"ls target/${REPO}-*-SNAPSHOT.jar",returnStdout: true, label: "Get Artifact").trim()
    if ( params.ARTIFACT != null) {
        ARTIFACT = params.ARTIFACT
    }
    environment {
        registryCredential = 'dockerhub'
    }
    docker.image("netfoundry/java-base:latest")
    def imageName = REPO
    if(REPO=="authorization") {
        imageName = "auth"
    }
    def dockerImage = docker.build ("netfoundry/${imageName}:${GIT_COMMIT}",
                    " --build-arg version=${VERSION}"+
                    " --build-arg bitbucket_commit=${GIT_COMMIT}" +
                    " --build-arg uid=99" +
                    " --build-arg gid=99" +
                    " --build-arg artifact=${ARTIFACT}"+
                    " --build-arg bitbucket_build_number=${BUILD_NUMBER} ."
                    )
    // println "Branch name is ${BRANCH_NAME}"
    if ( BRANCH_NAME == "develop") {
        TAGS = ["${GIT_COMMIT}", "sandbox", "latest"]
    } else if ( BRANCH_NAME ==~ /(hotfix.*)/ ){
        // println "Am in Hotfix branch"
        TAGS = ["${GIT_COMMIT}", NEW_BRNCH_PREFIX]
    } else {
        TAGS = ["${GIT_COMMIT}", NEW_BRNCH_PREFIX]
    }

    docker.withRegistry('', 'dockerhub') {
        for (int i =0; i< TAGS.size(); i++ ) {
            dockerImage.push(TAGS[i])
        }
    }    
}