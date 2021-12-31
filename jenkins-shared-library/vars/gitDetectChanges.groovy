#!/usr/bin/env groovy
/*
* Name: Git Detect Changes
* Author: Albert Mugisha
* Date: 02-20-2018
*/

def call(params) {
    def opts = params.opts ?: "sort"
    def changeSet = params.changeset ?: []
    def changedArray = []
    def newProjects = []
    for ( String project: changeSet) {
        newProjects.add(project.split("/")[0])
    }
    steps.sh "git diff --name-only HEAD^"
    def CHANGES = sh (script: "git diff --name-only HEAD^ | ${opts} ",returnStdout: true).trim()
    def CHANGED_ARRAY = CHANGES.split("\n")
    
    return changedArray
}