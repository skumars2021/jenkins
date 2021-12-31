#!/usr/bin/env groovy
/*
* Name: fetch_author
* Author: Vinay Lakshmaiah
* Date: 11-17-2021
*/

import groovy.json.JsonSlurper

def call(String repo, String commit_sha){
    try {
        commitUrl = "https://bitbucket.org/api/2.0/repositories/netfoundry/${repo}/commit"
        def commit_details_raw = bitbucket username: BITBUCKET_AUTH_USR, password: BITBUCKET_AUTH_PSW, endpoint: "${commitUrl}/${commit_sha}", output: true
        def commit_details = readJSON text: commit_details_raw['message']
        def author_fname = commit_details['author']['raw'].split(" ")[0]
        def author_lname = commit_details['author']['raw'].split(" ")[1]
        return "${author_fname} ${author_lname}"
    } catch(e) {
        echo "Error in getting author is ${e}"
        return null
    }
}