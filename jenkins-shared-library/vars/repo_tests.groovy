#!/usr/bin/env groovy
/*
* Name: repo_tests
* Author: Vinay Lakshmaiah
* Date: 11-30-2021
*/

import groovy.json.JsonSlurper
import org.apache.commons.lang3.StringUtils;
import hudson.model.*

def call(environment, REPO, NAMESPACE, set_artifact_url, set_smoketest_url, artifact_link, smoketest_url, failure_msg) {
    def failure = false
    try {
        labelledShell label: "Tests Prerequisites", script: """
            docker pull blazemeter/taurus:1.15.0
            tauruscli -version
        """
        
        def account_id = nfAWSAccounts account_name: "${environment}"
        dir("${REPO}/taurus"){
            withAWS(credentials: 'jenkins-central-mgt', region: 'us-east-1', role:'jenkins-central-mgt',  roleAccount: "${account_id}") {
                labelledShell label: 'Taurus SmokeTest - Click to view results', script: """
                    tauruscli -gatewayurl=https://gateway-${NAMESPACE}.${environment}.netfoundry.io -registrationurl=https://registration-${NAMESPACE}.${environment}.netfoundry.io -environment=${environment} -approve
                """
            }
            junit "artifacts/xunit.xml"
            archiveArtifacts "artifacts/request-response/*"
            archiveArtifacts "artifacts/jmeter.log"
        }
    } catch(e) {
        dir("${REPO}/taurus"){
            failure = true
            try {
                archiveArtifacts "artifacts/request-response/*"
                archiveArtifacts "artifacts/jmeter.log"
                def junit_results = junit "artifacts/xunit.xml"
            } catch(exmp) {
                echo "Failed to archive folders"
            }
            
            def traceId = "", networkid = ""
            def errorTest = sh ( script: "grep error artifacts/results.csv | head  -1  | cut -d, -f1 || echo none",returnStdout: true).trim()

            if( errorTest != "none") {
                traceId = sh (script: "grep 'X-B3-TraceId:' artifacts/request-response/${errorTest}.txt | tail -1 | cut -d: -f2",returnStdout: true).trim()
                set_artifact_url = true
                artifact_link = "${JENKINS_URL}job/${JOB_BASE_NAME}/${BUILD_NUMBER}/artifact/artifacts/request-response/${errorTest}.txt"
                if( traceId.length() > 0 ){
                    set_smoketest_url = true
                    def startDate = dateMath minutes: 240
                    def start_date = startDate.format( "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" )
                    def end_date = new Date().format( "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" )
                    def index_es = "829281f0-cc24-11ea-9a27-434d53c8806f"
                     if(environment == "production") {
                        index_es = "1a8f89c0-cd25-11ea-9a27-434d53c8806f"
                    }
                    smoketest_url = "https://bf915c25292eaced6cd2cca6ab8cd77f.us-east-1.aws.found.io:9243/app/kibana#/discover?_g=(time:(from:'${start_date}',mode:absolute,to:'${end_date}'))&_a=(columns:!(_source),index:'${index_es}',interval:auto,query:(language:lucene,query:'${traceId}%20AND%20Error'),sort:!('@timestamp',desc))&"
                }    
            }

            if( failure_msg != "ignore" ) {
                failure_msg = "Smoketest failed. Please check Smoketest link and/or the request/response link for more detail"
            }
        }
                    
        catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
            error "${e}"
        }
    }
    return [set_artifact_url, set_smoketest_url, artifact_link, smoketest_url, failure, failure_msg]
}
