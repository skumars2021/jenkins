#!/usr/bin/env groovy
/*
* Name: Check Sonar Quality Gate
* Author: Albert Mugisha
* Date: 06-17-2020
* Sonarscanner should not be executed in parallel jobs
*/

import groovy.json.JsonSlurper

def call(params) {    
    def PROJECT_NAME = params.PROJECT_NAME
    def BRANCH = params.BRANCH
    def TIMEOUT = params.TIMEOUT ?: 300
    def MAVEN_HOME = params.MAVEN_HOME ?: "/home/jenkins/.m2"
    def return_msg = ""
    def SONAR_ID = "io.netfoundry"
    def JAVA_HOME = params.JAVA_HOME ?: null
    def PROJECTS_DIFF_SONAR_ID = [
        "core-management": "netfoundry_core-management"
    ]
    def exports = ""
    if ( JAVA_HOME  != null) {
        exports = "export JAVA_HOME=${JAVA_HOME}; "
    }
    
    if(PROJECTS_DIFF_SONAR_ID.containsKey(PROJECT_NAME)){
        SONAR_ID = PROJECTS_DIFF_SONAR_ID[PROJECT_NAME]
    }

    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: "sonar", usernameVariable: 'SONAR_USR', passwordVariable: 'SONAR_PSW']]) {
        withSonarQubeEnv('SonarCloud') {
            labelledShell label: 'Sonar Analysis', script: """
                ${exports}mvn sonar:sonar -Dsonar.host.url=https://sonarcloud.io \
                -Dmaven.repo.local=${MAVEN_HOME} \
                -Dsonar.organization=netfoundry -Dsonar.projectName=${PROJECT_NAME} \
                -Dsonar.branch.name=${BRANCH} -Dsonar.login=${SONAR_PSW} 
            """
        }
        sleep 5
        try {
            timeout(time: "${TIMEOUT}", unit: 'SECONDS') {
                def stat = waitForQualityGate abortPipeline: false
                if(stat.getStatus() != "OK" ){
                    def branch_no_spaces = BRANCH.replaceAll("/","%2F")
                    return_msg = "https://sonarcloud.io/dashboard?id=${SONAR_ID}%3A${PROJECT_NAME}&branch=${branch_no_spaces}&resolved=false"
                    if(PROJECT_NAME == "core-management"){
                        return_msg = "https://sonarcloud.io/dashboard?id=${SONAR_ID}&branch=${branch_no_spaces}&resolved=false"
                    }
                }
            }
        } catch(e) {
            echo "Timed out while waiting for Analysis result. Will proceed with build.."
        }

        return return_msg
    }
}