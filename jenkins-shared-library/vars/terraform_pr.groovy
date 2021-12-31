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
  def execution = params.execution ?: "plan"

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
                 "terraform plan"
  command = "terraform init;"+
            "terraform workspace select ${workspace}; " +
            "terraform init;"+
            "terraform workspace list; "+
            "source ${auth_env}; "+
            "terraform apply -auto-approve" 
  def final_command = ""
  if(execution == "plan") {
    final_command = command_plan
  } else {
    final_command = command
  }
  
  dir("${directory}") {
    withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: "	aws_trial_opsmgt", variable: 'AWS_ACCESS_KEY_ID']]) {
      labelledShell label: "Terraform ${execution}", script: """
        ls -ltr
        rm -rf .terraform
        ${final_command}
      """
    }
  }
}
