#!/usr/bin/env groovy
/*
* Name: ECS Deployments - TF
* Author: Albert Mugisha
* Date: 02-19-2018
*/

def call( params ) {
  def parallelize = params.parallelize ?: true
  def services = params.services
  def environment = params.environment
  def exclusions = params.exclusions ?: []
  def deployments = [:]
  def action = params.action ?: "plan"
  def access_key = params.access_key
  def secret_access_key = params.secret_access_key
  def extras = params.extras ?: "pwd"
  def tfversion = "0.11.15"

  for( int z=0; z<services.size(); z++) {
    def service = services[z]
    def serviceStep = "${service}"
    def workspace = (environment == "sandbox") ? "nf-sandbox": "nf-${environment}-int"
    if( environment == "production"){
      workspace = "nf-prod-int"
    }
    if( action == "apply") {
      action = "apply -auto-approve"
    }
    if ( !(service in exclusions)) {
      if (service == "api-gateway") { service = "gateway" }
      def command = "tfenv use ${tfversion}; "+
                    "terraform workspace list; "+
                    "terraform ${action}"
      if (serviceStep == "network") {
        def buildTF = [ "data", "edge", "engine", "infra"]
        for( String build: buildTF) {
          steps.dir("${build}/terraform") {
            withEnv([ "AWS_ACCESS_KEY_ID=${access_key}", "AWS_SECRET_ACCESS_KEY=${secret_access_key}"]) {
              steps.echo "Terraform for ${build}"
              sh "${extras}; rm -rf .terraform; tfenv use ${tfversion}; terraform init;  terraform workspace select ${workspace}; terraform init; tfenv use ${tfversion}; source ../../auth-env.sh && ${command}"
            }
          }
        }
      }else {
        steps.dir("terraform") {
          withEnv([ "AWS_ACCESS_KEY_ID=${access_key}", "AWS_SECRET_ACCESS_KEY=${secret_access_key}"]) {
            sh "${extras}; rm -rf .terraform; tfenv use ${tfversion}; terraform init;  terraform workspace select ${workspace}; terraform init; tfenv use ${tfversion}; source ../auth-env.sh && ${command}"
          }
        }
      }
    } 
  }
}
