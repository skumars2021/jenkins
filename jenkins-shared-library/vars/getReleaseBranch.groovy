#!/usr/bin/env groovy
/*
* Name: getReleaseBranch
* Author: Vinay Lakshmaiah
* Date: 11-19-2021
*/

import groovy.json.JsonSlurper

def call(String repo, String commitHash, String apiUrl) {
    // Create Branch from CommitHash
    def release = "release/${commitHash}-${new Date().format( 'yyyyMMddHHmm' )}"
    def createBranchUrl = apiUrl+"/2.0/repositories/netfoundry/${repo}/refs/branches"
    def commitsUrl = apiUrl+"/2.0/repositories/netfoundry/${repo}/refs/branches/${release}"
    def description = "Back merge ${commitHash}"
    
    def commitResult = bitbucket endpoint: commitsUrl, method: "GET",  data: [:], description: "Commit Updates", username: BITBUCKET_AUTH_USR, password: BITBUCKET_AUTH_PSW, output: "true"
    if( commitResult['resultCode'] == 404 ) {
        def data = [
            "name" : "${release}",
            "target" : [ "hash" : commitHash ]
            ]
        def createResult = bitbucket endpoint: createBranchUrl, method: "POST",  data: data, description: "Commit Updates", username: BITBUCKET_ADMIN_USR, password: BITBUCKET_ADMIN_PSW, output: "true"
        if ( createResult["resultCode"] > 300) {
          error("Creating Branch failed with error: "+createResult["resultCode"])
        }
        def statusBuild = "INPROGRESS"
        timeout(time: "1200", unit: 'SECONDS') {
            while( statusBuild == "INPROGRESS" || statusBuild == null) {
                description = "Get Build Status for ${release}"
                def bitbucket_key = org.apache.commons.codec.digest.DigestUtils.md5Hex(release)
                def statusUrl = apiUrl+"/2.0/repositories/netfoundry/${repo}/commit/${commitHash}/statuses/build/${bitbucket_key}"
                data = [:]
                def statusResult = bitbucket endpoint: statusUrl, method: "GET",  data: [:], description: "Commit Updates", username: BITBUCKET_AUTH_USR, password: BITBUCKET_AUTH_PSW, output: "true"
                def statusResultJSON = readJSON text: statusResult["message"]
                statusBuild = statusResultJSON["state"]
                statusResultJSON = ""
                println "Waiting for build to complete: Current Status: ${statusBuild}"
                def successfulStatus = [ "INPROGRESS", "SUCCESSFUL", null ]
                if(statusResult['message'].contains("Changeset not found")) {
                    return
                }
                if( ! successfulStatus.contains(statusBuild)) {
                    error("Build for branch ${release} failed with Bitbucket Status: ${statusBuild}. Aborting pipeline.")
                }
                sleep 10
            }
        }
    } else if( commitResult['resultCode'] < 300 ) {
        println "Branch Already Exists, proceed to next stage"
    } else {
        error("Pipeline Aborted, received ${commitResult['resultCode']}")
    }
    return release
}