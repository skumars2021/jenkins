#!/usr/bin/env groovy
/*
* Name: detect_flyway_changes
* Author: Vinay Lakshmaiah
* Date: 11-25-2021
*/

import groovy.json.JsonSlurper

def call(String repo, String sha1, String sha2) {
    dir("${repo}"){
        def res = false
        try {
            def CHANGES = sh (script: "git diff --name-only ${sha1} ${sha2}",label:"Saving Git diff to a variable",returnStdout: true).trim()
            def CHANGED_ARRAY = CHANGES.split("\n")
            for( String chk_array: CHANGED_ARRAY) {
                if( chk_array.contains("migration")) {
                    labelledShell label: "SQL Migration detected.", script: """
                        echo 'Flyway migration changes detected.'
                        echo '${chk_array}'
                    """
                    res = true
                }    
            }
        } catch(e) {
            res = true
        }
        return res
    }
}