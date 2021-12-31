#!/usr/bin/env groovy
/*
* Name: getReleaseBranch
* Author: Vinay Lakshmaiah
* Date: 11-19-2021
*/

import groovy.json.JsonSlurper

def call(String repo, String commit_hash, String dep_env) {
    def image = "netfoundry/${repo}:${commit_hash}"
    def service = repo
    if(repo == "api-gateway") { service = "gateway" }  
    if(repo == "authorization") { service = "auth"; image = "netfoundry/auth:${commit_hash}" }

    def account_id = nfAWSAccounts account_name: "${dep_env}"
    if(repo == "network"){
        def services = ["data", "engine", "infra", "edge"]
        def buildServices = [:]
        for( String svc: services) {
            def serviceStep = "${svc}"
            def ecs_service_name = serviceStep
            def image_svc = "netfoundry/${serviceStep}:${commit_hash}"
            buildServices[serviceStep] = { ->
                withAWS(credentials: 'jenkins-central-mgt', region: 'us-east-1', role:'jenkins-central-mgt',  roleAccount: "${account_id}") {
                    sh "ecs deploy --region us-east-1 --image ${serviceStep}-service ${image_svc} --timeout 600 ecs-cluster-nfn-internal-env-private ecs-${ecs_service_name}-service"
                } 
            }
        }
                            
        parallel buildServices
    } else {
        def ecs_svc_name = service
        def container_name = "${service}-service"
        if(service == "gateway"){
            ecs_svc_name = "fargate-gateway"
            
        }
        if(service == "registration"){
            ecs_svc_name = "fargate-registration"
            container_name = "registration"
        }
        if(service == "salt-data-service") {
            ecs_svc_name = "sds"
            container_name = "${service}"
        }
        withAWS(credentials: 'jenkins-central-mgt', region: 'us-east-1', role:'jenkins-central-mgt',  roleAccount: "${account_id}") {
            sh "ecs deploy --region us-east-1 --image ${container_name} ${image} --timeout 600 ecs-cluster-nfn-internal-env-private ecs-${ecs_svc_name}-service"
        }
    }
}