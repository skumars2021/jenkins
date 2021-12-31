#!/usr/bin/env groovy
/*
* Name: Artifactory
* Author: Albert Mugisha
* Date: 02-21-2018
*/

def call(params) {
    def buildServices = [:]
    def services = params.services ?: []
    def parallelize = params.parallelize ?: false
    def exclusions = params.exclusions ?: []
    def failBuild = params.failBuild ?: false
    def switch_dir = params.switch_dir ?: true
    def xml = params.xml
    def failThreshold = params.failThresholdPercent ?: 0
    
    for( int z=0; z<services.size(); z++) {
        def service = services[z]
        def serviceStep = "${service}"
        if ( !(service in exclusions)) {
            steps.echo "Junit for ${service}"
            if ( parallelize == true) {
                buildServices[serviceStep] = { ->
                  junit(service, failBuild, failThreshold, xml, switch_dir)
                }
            } else {
              junit(service, failBuild, failThreshold, xml, switch_dir)
            }
        }
    }

    if( parallelize == true ) {
        steps.parallel buildServices
    }
}


def junit(service, failBuild, failThreshold, xml, switch_dir) {
  def buildResult = true
  if ( switch_dir == true) {
    dir("${service}") {
      buildResult = junit_run(service, failBuild, failThreshold, xml)       
    }
  } else {
    buildResult = junit_run(service, failBuild, failThreshold, xml)
  }

  if( buildResult == false) {
    sh "exit 1"
  }
}

def junit_run(service, failBuild, failThreshold, xml) {
  def buildResult = true
  try {
    def junit_result = junit "${xml}"
    if( failThreshold != 0) {
      def percentage = junit_result.getFailCount() / junit_result.getTotalCount()
      steps.echo "Failure percentage: ${percentage}, Failure Threshold: ${failThreshold}"
      if ( percentage > failThreshold) {
        buildResult = false
        error("You have ${junit_result.getFailCount()} failing tests. Failure threshold is set to ${failThreshold}%") 
      }
      }
    } catch(err) {
      steps.echo "Failed to parse junit reports with error ${err}"
      if( failBuild == true) {
        buildResult = false
        error("Build Failed due to Junit Failure")
      }
    }

    return buildResult
}