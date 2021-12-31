#!/usr/bin/env groovy
/*
* Name: Start and Stop Node
* Author: Albert Mugisha
* Date: 02-20-2018
*/
import groovy.json.*

def call(params) {
    def url = params.url
    def access_token = params.access_token ?: null
    def time_out = params.timeout ?: 120
    def data = params.data ?: []

    def jsonData = new JsonBuilder( data ).toPrettyString()

    def headers = [
        [
            name:  'Authorization',
            value: "Bearer ${access_token}",
            maskValue: true
        ]
    ]

    def response = httpRequest url: url, httpMode: 'POST', timeout: 120, requestBody: jsonData, customHeaders: headers, consoleLogResponseBody: true, contentType: 'APPLICATION_JSON'

    print response
    def return_response = new groovy.json.JsonSlurperClassic().parseText(response.content)

    return return_response
}