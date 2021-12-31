#!/usr/bin/env groovy
/*
* Name: check_tf_changes
* Author: Vinay Lakshmaiah
* Date: 11-16-2021
*/

import groovy.json.JsonSlurper

def call(String tf_directory, String sha1, String sha2) {
    def result = false
    try {
        dir(tf_directory){
            if( sha2 == "production" || sha2 == "staging") {
                sha2 = "master"
            }
            labelledShell label: 'Git Diff', script: """
                git diff --name-only ${sha1} ${sha2}
            """
            def CHANGES = sh (script: "git diff --name-only ${sha1} ${sha2}",returnStdout: true).trim()
            def CHANGED_ARRAY = CHANGES.split("\n")
            echo "${CHANGED_ARRAY}"
            for( String chk_array: CHANGED_ARRAY) {
                if( chk_array.contains("terraform")) {
                    echo "Terraform changes detected"
                    result = true
                }
            }
        }
    } catch(e) {
        echo "Git Diff did not return results, proceeding and assuming TF changes.."
        result = true
    }
    return result
}