#!/usr/bin/env groovy
/*
* Name: ECS Deployments - TF
* Author: Albert Mugisha
* Date: 02-19-2018
*/

def call( params ) {
  def directory = params.directory ?: "terraform"
  def environment = params.environment 
  def auth_env = params.auth_env ?: "../auth-env.sh"
  def commit_hash = params.commit_hash ?: "sandbox"

  def workspace = (environment == "sandbox") ? "nf-sandbox": "nf-${environment}-int"

  if( environment == "production"){
    workspace = "nf-prod-int"
  }
  def command = ""
  def command_plan = ""
  command_plan = "terraform init;"+
                 "terraform workspace select ${workspace}; " +
                 "terraform init;"+
                 "terraform workspace list; "+
                 "source ${auth_env}; "+
                 "terraform -v; "+
                 "terraform plan -var commit_hash=${commit_hash}"
  command = "terraform init;"+
            "terraform workspace select ${workspace}; " +
            "terraform init;"+
            "terraform workspace list; "+
            "source ${auth_env}; "+
            "terraform apply -auto-approve -var commit_hash=${commit_hash}" 
  
  dir("${directory}") {
    withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: "	aws_trial_opsmgt", variable: 'AWS_ACCESS_KEY_ID']]) {
      sh "ls -ltr"
      sh "rm -rf .terraform"
      sh "${command_plan}"
      sh "${command}"
  }
  }
}
