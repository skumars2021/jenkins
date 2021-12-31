#!/usr/bin/env groovy
/*
* Name: Find Free Node
* Author: Albert Mugisha
* Date: 02-20-2018
*/

def call( params ) {
    def clustername = params.clustername
    def deploy_environment = params.deploy_environment
    def SLEEP = params.SLEEP ?: 10
    def state = params.state 
    def account_aliases = [
        "sandbox": "nf-sandbox",
        "integration": "nf-integration-internal",
        "staging": "nf-stage-internal",
        "trial": "nf-trial-int",
        "private": "nf-prod-private-internal",
        "production": "nf-prod-internal"
    ]
    
    confirmEnvJSON = ""
    withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: "aws_${deploy_environment}", variable: 'AWS_ACCESS_KEY_ID']]) {
        int new_max_capacity, new_desired_capacity, new_min_capacity;
        confirmEnvJSON = ""
        if (state == "up") {
            def asg = sh (script: "aws autoscaling describe-auto-scaling-groups --auto-scaling-group-names ${clustername} --region us-east-1 --query 'AutoScalingGroups[*].{DesiredCapacity:DesiredCapacity,MaxSize:MaxSize,MinSize:MinSize}' --output text",returnStdout: true).trim()
            asg_desired_capacity = asg.split("\t")[0] as Integer
            asg_max_capacity = asg.split("\t")[1] as Integer
            asg_min_capacity = asg.split("\t")[2] as Integer
            new_max_capacity =  asg_max_capacity * 2
            new_desired_capacity =  asg_desired_capacity * 2
            new_min_capacity =  asg_min_capacity * 2
        } else {
            new_max_capacity =  asg_max_capacity
            new_desired_capacity =  asg_desired_capacity
            new_min_capacity =  asg_min_capacity
        }

        sh "aws autoscaling update-auto-scaling-group --max-size ${new_max_capacity} --min-size ${new_min_capacity} --desired-capacity ${new_desired_capacity} --auto-scaling-group-name ${clustername} --region us-east-1 --output text"
        def status = false
        def asg_array = [:]

        while ( status == false ) {
            status = true
            def asg2 = sh (script: "aws autoscaling describe-auto-scaling-instances --query 'AutoScalingInstances[*].[LifecycleState,AutoScalingGroupName]' --region us-east-1 --output json",returnStdout: true).trim()
            def asg_json = stringToJSON str: asg2
            def counter = 0
            def scaled = 0
            for(int x = 0; x < asg_json.size(); x++ ) {
                if(clustername in asg_json[x]) {
                    counter = counter + 1
                    if (! "InService" in asg_json[x] ) {
                        println "Not all instances are in Inservice yet..."
                        status = false
                    } else {
                        scaled = scaled +1
                    }
                }
            }
            println "Current instances: ${counter}, No. scaled: ${scaled}"
            

            if ( state == "up" ) {
                if ( counter < new_desired_capacity ) {
                    println "Instances not scaled up yet... "
                    status = false
                }
            } else {
                if ( counter > new_desired_capacity ) {
                    println "Instances not scaled down yet... "
                    status = false
                }
            }
            asg2 = ""
            asg_json = ""

            sleep SLEEP
          }

          println "Scaling Activity done. All EC2 are in service"
      }
  }