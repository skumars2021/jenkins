#!/usr/bin/env groovy
/*
* Name: gitClone
* Author: Vinay Lakshmaiah
* Date: 11-17-2021
*/

import groovy.json.JsonSlurper

def call(String commit_hash, String repo) {
    // Clone GitHash
    sh "mkdir -p ${repo}"
    dir("${repo}"){
        checkout ( [$class: 'GitSCM',
            branches: [[name: "${commit_hash}" ]],
            userRemoteConfigs: [[
                credentialsId: 'afd18d9b-3d41-4b59-b5cc-05cd7df2ce63', 
                url: "git@bitbucket.org:netfoundry/${repo}.git"
        ]]])
    }
}