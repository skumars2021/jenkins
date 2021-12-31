#!/usr/bin/env groovy
/*
* Name: fetch_current_deployed_container
* Author: Vinay Lakshmaiah
* Date: 11-17-2021
*/

import groovy.json.JsonSlurper

def call(String repo, String environment) {
    def account_id = nfAWSAccounts account_name: "${environment}"
    
    withAWS(credentials: 'jenkins-central-mgt', region: 'us-east-1', role:'jenkins-central-mgt',  roleAccount: "${account_id}") {        
        def container = repo
        if(repo == "network") { container = "data"}
        if(repo == "authorization") { container = "auth"}
        if(repo == "registration") { container = "fargate-registration"}
        if(repo == "api-gateway") { container = "fargate-gateway"}
        if(repo == "salt-data-service") { container = "sds"}
        def ecs_service = "arn:aws:ecs:us-east-1:${account_id}:service/ecs-${container}-service"
        def CLUSTER = "ecs-cluster-nfn-internal-env-private"
        def taskDefinition = sh (script: "aws ecs describe-services --region us-east-1 --services ${ecs_service} --cluster ${CLUSTER} --query 'services[*].taskDefinition' --output text",label: "Retrieve Task Definition", returnStdout: true).trim()
        def image = sh (script: "aws ecs describe-task-definition --task-definition ${taskDefinition}  --region us-east-1 --query 'taskDefinition.containerDefinitions[*].image' --output text",label: "Read Running Container Image",returnStdout: true).trim()
        return image.split(":")[1]
    }
}