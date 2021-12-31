#!/usr/bin/env groovy
/*
* Name: Veracode Scan
* Author: Evan Gertis
* Date: 08-30-2021
*/

import groovy.json.JsonSlurper

def call(params) {    
    def REPO = params.REPO
    sh "curl -sSL https://download.sourceclear.com/ci.sh | sh -s scan --image \$(docker images --format '{{.Repository}}:{{.Tag}}' | grep netfoundry/${REPO}:release | head -1)" 
}