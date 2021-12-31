#!/usr/bin/env groovy
/*
* Name: Bitbucket Update
* Author: Albert Mugisha
* Date: 02-20-2018
*/

import groovy.json.*

def call(params) {
    def authUrl = params.authUrl
    def data = params.data ?: [:]
    def verbose = params.verbose ?: "true"

    def jsonData = new JsonBuilder( data ).toPrettyString()
    def response = httpRequest(
        url: authUrl,
        httpMode: 'POST',
        timeout: 30,
        requestBody: jsonData,
        contentType: "APPLICATION_JSON",
        consoleLogResponseBody: true
    )
    def responseJSON =  new groovy.json.JsonSlurperClassic().parseText(response.content)
    return responseJSON['access_token']
}
