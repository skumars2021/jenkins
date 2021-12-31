#!/usr/bin/env groovy
/*
* Name: ECS Deployments - ECS Deploy
* Author: Albert Mugisha
* Date: 02-19-2018
*/

def call( params ) {
  def parallelize = params.parallelize ?: true
  def services = params.services
  def environment = params.environment
  def exclusions = params.exclusions ?: []
  def deployments = [:]
  def container = params.container ?: ""
  def deployment_mechanism = params.deployment_mechanism ?: "restart_container"

  if(deployment_mechanism == "container" && container == "") {
    throw new Exception('If chosen deploy method is container, please provide container to be deployed ')
  }
  
  for( int z=0; z<services.size(); z++) {
    def service = services[z]
    def serviceStep = "${service}"
    if ( !(service in exclusions)) {
      if (service == "authorization/auth") { service = "auth" }
      if (service == "api-gateway") { 
        service = "ecs-fargate-gateway-service"
        if( environment == "private") {
          service = "ecs-gateway-internal-service" 
        }
      } else {
        service = "ecs-${service}-service" 
      }

      if(service == "ecs-registration-service") {
        service = "ecs-fargate-registration-service"
      }
      def command = "ecs deploy --region us-east-1 --timeout 600 ecs-cluster-nfn-internal-env-private ${service}"

      if ( deployment_mechanism == "container" && container != "default") {
        def image = "netfoundry/${serviceStep}:${container}"
        def ecs_container_name = serviceStep
        if(serviceStep == "api-gateway") {
          ecs_container_name = "gateway"
        }  
        command = "ecs deploy --region us-east-1 --image ${ecs_container_name}-service ${image} --timeout 600 ecs-cluster-nfn-internal-env-private ${service}"
      }
      
      if (parallelize == true) {
        deployments[serviceStep] = { ->
          withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: "aws_${environment}", variable: 'AWS_ACCESS_KEY_ID']]) {
            // confirmEnv(environment)
            sh "${command}"
          }
        }
      } else {
        withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: "aws_${environment}", variable: 'AWS_ACCESS_KEY_ID']]) {
          // confirmEnv(environment)
          sh "${command}"
        }
      } 
    } 
  }

  if( parallelize == true) {
    steps.parallel deployments
  }
}

def confirmEnv(deployment_env) {
  def account_aliases = [
        "sandbox": "nf-sandbox",
        "integration": "nf-integration-internal",
        "staging": "nf-stage-internal",
        "trial": "nf-trial-int",
        "private": "nf-prod-private-internal",
        "production": "nf-prod-internal"
    ]
  def confirmEnv = sh (script: "aws iam list-account-aliases",returnStdout: true).trim()
  def confirmEnvJSON = stringToJSON str: confirmEnv
  steps.echo confirmEnv
  if(confirmEnvJSON["AccountAliases"][0] != account_aliases[deployment_env]) {
    steps.error("Pipeline aborting.. You are deploying to the wrong environment.")
    sh "exit 1"
  }
  confirmEnvJSON = ""
}
