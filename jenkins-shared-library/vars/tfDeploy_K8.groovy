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
  def image = params.image


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
      def directory = service
      def command = "tfenv use 0.11.13; "+
                    "terraform workspace list; "+
                    "terraform ${action} -var image=${image}"
      if (serviceStep == "network") {
        def buildTF = [ "data", "edge", "engine", "infra"]
        for( String build: buildTF) {
          steps.dir("${build}/terraform") {
            steps.echo "Terraform for ${build}"
            sh "${extras}; rm -rf .terraform; terraform init;  terraform workspace select ${workspace}; terraform init; source ../../auth-env.sh && ${command}"
          }
        }
      }else {
        steps.dir("terraform") {
          sh "${extras}; rm -rf .terraform; terraform init; terraform workspace select ${workspace}; terraform init; source ../auth-env.sh && ${command}"
        }
      }
    }
  } 
}
