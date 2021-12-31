#!/usr/bin/env groovy
/*
* Name: Start and Stop Node
* Author: Albert Mugisha
* Date: 02-20-2018
*/

def call(params) {
    def project = params.project
    def action = params.action
    def node = params.node
    def slavepool = params.slavepool
    return sh (script: "/home/jenkins/jenkins_pipelines/jenkins_job_check.sh ${project} ${action} ${node} \"${slavepool}\"",returnStdout: true).trim()
}