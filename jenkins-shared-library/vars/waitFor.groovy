#!/usr/bin/env groovy
/*
* Name: Start and Stop Node
* Author: Albert Mugisha
* Date: 02-20-2018
*/

def call(params) {
    def url = params.url
    def res_status = params.status ?: 200
    def access_token = params.access_token ?: null
    def SLEEP = params.sleep ?: 10
    def time_out = params.timeout ?: 600
    print url
    def result = [:]
    def data = [:]
    def status = 0
    steps.timeout(time: "${time_out}", unit: 'SECONDS') {
        while ( "${status}" != "${res_status}") {
            def headers = [
                [
                    name:  'Authorization',
                    value: "Bearer ${access_token}",
                    maskValue: true
                ]
            ]
            def response = httpRequest(
                url: url,
                httpMode: 'GET',
                timeout: time_out,
                customHeaders: headers,
                consoleLogResponseBody: true
            )
            steps.echo "${response}"
            def getResultJSON = new groovy.json.JsonSlurperClassic().parseText(response.content)
            status = getResultJSON["status"]
            steps.echo "Expecting Status: ${res_status}, Current Status: ${status}"
            result = ""
            if( status == 500 ) {
                steps.echo "Status ERROR (500) Received: Build is aborted"
                error("Status ERROR (500) Received: Build is aborted")
                sh "exit 1"
            }
            sleep SLEEP
        }
    }
}