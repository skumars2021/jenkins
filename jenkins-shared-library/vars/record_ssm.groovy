#!/usr/bin/env groovy
/*
* Name: record_ssm
* Author: Vinay Lakshmaiah
* Date: 11-17-2021
*/

import groovy.json.JsonSlurper

def call(String repo, String dep_env, String commit_hash){
    def service_name = repo
    if( repo == "authorization") { service_name = "auth"}
    if( repo == "salt-data-service") { service_name = "sds"}
    def account_id = nfAWSAccounts account_name: "${dep_env}"
    withAWS(credentials: 'jenkins-central-mgt', region: 'us-east-1', role:'jenkins-central-mgt',  roleAccount: "${account_id}") {
        if( params.REPO != "network"){
            sh "aws ssm put-parameter --name /${dep_env}/container-image/${service_name}-service --type String --value ${commit_hash} --region us-east-1 --overwrite"
        } else {
            for(String svc: ["infra", "engine", "edge", "data"]) {
                sh "aws ssm put-parameter --name /${dep_env}/container-image/${svc}-service --type String --value ${commit_hash} --region us-east-1 --overwrite"
            }
        }
    }
}