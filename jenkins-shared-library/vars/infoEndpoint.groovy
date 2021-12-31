#!/usr/bin/env groovy
/*
* Name: Find Free Node
* Author: Albert Mugisha
* Date: 02-20-2018
*/

def call( params ) {
    def services = params.services
    def branch = params.branch ?: ""
    def deployment_env = params.deployment_env
    def exclusions = params.exclusions ?: []
    def git_commit = params.git_commit ?: ""
    def verify_branch = params.verify_branch ?: false
    def verify_commit = params.verify_commit ?: false
    def timeout = params.timeout ?: 120
    def verify_endpoint = [:]
    for( int z=0; z<services.size(); z++) {
        def service = services[z]
        if ( !(service in exclusions)) {
            def endpoint = "${service}"
            def url = "https://${endpoint}.${deployment_env}.netfoundry.io/info"
            def httpParams = [
                    "endpoints": [url],
                    "stage_names": [endpoint],
                    "method": "GET",
                    "description": "Info Endpoint Verification",
                    "exit_on_error": true
                ]
            verify_endpoint[endpoint] = { ->
                waitForHttpResponse url: url, timeout: timeout, status: 200 
                def endPointResult = httpEndpoint(httpParams)
                if (endPointResult[0]["resultCode"] > 300) {
                    error("Info Endpoint Verification failed")
                    sh "exit 1"
                }
                def endPointResultJSON = new groovy.json.JsonSlurper().parseText(endPointResult[0]["message"])
                println "Branch Verification: ${verify_branch}"
                if (verify_branch == true) {
                    if ( endPointResultJSON["git"]["branch"] != branch ) {
                        error("Info Endpoint doesnt match deployed branch")
                        sh "exit 1"
                    }
                }
                
                if ( verify_commit == true ) {
                    if ( endPointResultJSON["git"]["commit"]["id"] != git_commit  ) {
                        println "${git_commit}"
                        println "${endPointResultJSON["git"]["commit"]["id"]}"
                        error("Git Sha doesnt match")
                        sh "exit 1"
                    }
                }
                
                endPointResult = ""
                endPointResultJSON = ""
            }
        }
    }
    parallel verify_endpoint
}