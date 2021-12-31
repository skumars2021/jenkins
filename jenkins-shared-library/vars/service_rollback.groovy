#!/usr/bin/env groovy
/*
* Name: service_rollback
* Author: Vinay Lakshmaiah
* Date: 11-26-2021
*/

import groovy.json.JsonSlurper

def call(namespace, environment, killed_running_job){
    // Helm Cleanout
    def account_id = nfAWSAccounts account_name: "${environment}"
    try {
        run_eks_command("for chart in \$(helm ls --namespace ${namespace} | cut -f1 | grep -v NAME);  do helm delete \$chart --namespace ${namespace}; done || true", environment, "Cleanup - delete helm charts")
    } catch(e) {
        echo "Failed to Cleanout Helm ${e}"
    }

    // Namespace Delete
    try {
        run_eks_command("kubectl delete namespace ${namespace} || true", environment, "Cleanup - Delete Namespace")
    } catch(e) {
        echo "Failed to Cleanout Namespace ${e}"
    }

    // SQS
    try {
        withAWS(credentials: 'jenkins-central-mgt', region: 'us-east-1', role:'jenkins-central-mgt',  roleAccount: "${account_id}") {
            def check_if_queue_exists = sh ( script: "aws sqs get-queue-url --queue-name ${namespace} --region us-east-1 --output text || echo none", label: "Check if SQS queue exists", returnStdout: true)
            if (check_if_queue_exists.contains("none")) {
                labelledShell label: "Cleanup any running queues for namespace ${namespace}", script: """
                    aws sqs delete-queue --region us-east-1 --queue-url https://queue.amazonaws.com/721086286010/${namespace} || true
                """
                if ( killed_running_job == true ) {
                    sleep 60
                }
            }
        }
    } catch(e) {
        echo "Failed to Cleanout SQS"
    }

    // ASG
    try {
        withAWS(credentials: 'jenkins-central-mgt', region: 'us-east-1', role:'jenkins-central-mgt',  roleAccount: "${account_id}") {
            labelledShell label: "Update AutoScaling Group - ${namespace} to have zero running instances", script: """
                aws autoscaling update-auto-scaling-group --max-size 0 --min-size 0 --desired-capacity 0 --auto-scaling-group-name ${namespace} --region us-east-1 --output text || true
            """
            counter = 1
            needed_cleanup = false
            while( counter.toInteger() > 0 ) {
                counter = sh (script: "aws autoscaling describe-auto-scaling-instances --query 'AutoScalingInstances[*].[LifecycleState,AutoScalingGroupName]' --region us-east-1 --output json | grep ${namespace} | wc -l", label: "Get running EC2 instances for ${namespace} Autoscaling group", returnStdout: true).trim()
                if( counter.toInteger() != 0) {
                    echo "Number of EC2 instances: ${counter}, expected count 0"
                    sleep 10
                }
                if( counter.toInteger() > 0 ) {
                    needed_cleanup = true
                }
            }
            labelledShell label: "Cleanup Autoscaling group -  ${namespace}", script: """
                aws autoscaling delete-auto-scaling-group --auto-scaling-group-name ${namespace} || true
            """
            if( needed_cleanup == true ) {
                sleep 10
            }
        }
    } catch(e) {
        echo "Failed to Cleanout ASG"
    }
    
}