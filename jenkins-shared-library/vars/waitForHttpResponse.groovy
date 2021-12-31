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
    steps.echo url
    def result = [:]
    def data = [:]
    def status = ""
    steps.timeout(time: "${time_out}", unit: 'SECONDS') {
        while ( "${status}" != "${res_status}") {
            result = httpEndpoint endpoints:[url], description: "Wait For Status", access_token: access_token
            status = result[0]["resultCode"]
            steps.echo "Status is ${status}"
            result = ""
            sleep SLEEP
        }
    }
}