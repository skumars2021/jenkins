#!/usr/bin/env groovy
/*
* Name: scale_cluster
* Author: Vinay Lakshmaiah
* Date: 11-24-2021
*/

import groovy.json.JsonSlurper
import org.apache.commons.lang3.StringUtils;
import hudson.model.*

def call(environment, capacity, namespace, repo, cluster_name) {
    def account_id = nfAWSAccounts account_name: "${environment}"
    withAWS(credentials: 'jenkins-central-mgt', region: 'us-east-1', role:'jenkins-central-mgt',  roleAccount: "${account_id}") {
        def asg_name = sh(script: "aws ssm get-parameter --name eks_asg_name --region us-east-1 --query 'Parameter.Value' --output text",label:"Retrieve Launch Configuration",returnStdout: true).trim()
        def private_subnets = sh(script: "aws ssm get-parameter --name eks_private_subnets --region us-east-1 --query 'Parameter.Value' --output text",label:"Retrieve Private subnets for EKS cluster",,returnStdout: true).trim()
        def asg = sh (script: "aws autoscaling describe-auto-scaling-groups --auto-scaling-group-names ${namespace} --region us-east-1  --query 'AutoScalingGroups[*].AutoScalingGroupName' --output text",label:"Check if ASG already exists",returnStdout: true).trim()
        if( repo == "network") { capacity = capacity + 1 }
        if (asg != namespace) {
            labelledShell label: "Creating ASG with name ${namespace}", script: """
                aws autoscaling create-auto-scaling-group --auto-scaling-group-name ${namespace} --launch-configuration-name ${asg_name} --min-size ${capacity} --max-size ${capacity} --desired-capacity ${capacity} --health-check-grace-period 300 --region us-east-1  --vpc-zone-identifier ${private_subnets} --tags Key=Name,Value=${namespace},PropagateAtLaunch=true Key=kubernetes.io/cluster/eks-cd-${environment},Value=owned,PropagateAtLaunch=true
                sleep 30
            """
            def scaling = true
            def instancesRaw = sh(script:"aws autoscaling describe-auto-scaling-groups --auto-scaling-group-names ${namespace} --region us-east-1 --query 'AutoScalingGroups[0].Instances[].InstanceId' --output text",label: "Query ASG Status",returnStdout: true).trim()
            sh script: "echo 'EC2 Instances returned ${instancesRaw}'", label: "ASG EC2 Instances"
            timeout(time: "600", unit: 'SECONDS') {
                def wait_instance_count = true
                while(wait_instance_count == true ) {
                    wait_instance_count = false
                    def instancesRawCount = sh(script:"aws autoscaling describe-auto-scaling-groups --auto-scaling-group-names ${namespace} --region us-east-1 --query 'AutoScalingGroups[0].Instances[].InstanceId'",label: "Get ASG Instance Count",returnStdout: true).trim()
                    def instancesRawJson = readJSON text: instancesRawCount
                    if(instancesRawJson.size() < capacity ) {
                        labelledShell label: "Waiting for instance count to get to ${capacity}, current count is ${instancesRawJson.size()}", script: "echo 'Waiting for instances to reach 3'" 
                        wait_instance_count = true
                        sleep 5
                    }  else {
                        wait_instance_count = false
                        labelledShell label: "Reached target instance count", script: "echo 'Reached target instance count'" 
                    }
                }
            }
            instancesRawCount = sh(script:"aws autoscaling describe-auto-scaling-groups --auto-scaling-group-names ${namespace} --region us-east-1 --query 'AutoScalingGroups[0].Instances[].InstanceId'",label: "Get ASG Instance Count",returnStdout: true).trim()
            instancesRawJson = readJSON text: instancesRawCount
            if(instancesRawJson.size() < capacity ) {
                error "Autoscaling failed. Pipeline will abort."
            }
        } else {
            echo "Cluster is already scaled."
        } 
    }
    sleep 10
    //Label Nodes
    def namespace_nodes = []
    withAWS(credentials: 'jenkins-central-mgt', region: 'us-east-1', role:'jenkins-central-mgt',  roleAccount: "${account_id}") {
        def namespace_nodes_raw = sh (script: "aws ec2 describe-instances --region us-east-1 --filter Name=tag:Name,Values=${namespace} --query 'Reservations[*].Instances[*].PrivateDnsName' --output json",label: "Get ASG EC2 Private DNS Names",returnStdout: true).trim()
        namespace_nodes = readJSON text: namespace_nodes_raw

        def check_nodes_ready = true
        timeout(time: "600", unit: 'SECONDS') {
            while(check_nodes_ready) {
                def JSONPATH='{range .items[*]}{@.metadata.name}:{range @.status.conditions[*]}{@.type}={@.status};{end}{end}'
                def raw_output = sh (script: "kubectl config use-context ${cluster_name} > /dev/null; kubectl get nodes -o jsonpath=\"${JSONPATH}\" | grep \"Ready=True\" ",label: "Checking if ASG EC2 nodes are registered with EKS",returnStdout: true).trim()
                for( String[] nodes: namespace_nodes) {
                    for (String node: nodes) {
                        if(!raw_output.contains(node)) {
                            check_nodes_ready = true
                            sleep 5
                        } else {
                            check_nodes_ready = false
                        }
                    }
                }
            }
        }

        for( String[] nodes: namespace_nodes) {
            // Label Node
            for (String node: nodes) {
                if(node.size() > 5) {
                    try {
                        sh script: "kubectl label --overwrite=true nodes ${node} deployment=${namespace}", label: "Label EC2 ${node} with label 'deployment=${namespace}'"
                    } catch(exmpl) {
                        sleep 5 
                        sh script: "kubectl label --overwrite=true nodes ${node} deployment=${namespace} || true", label: "Label EC2 ${node} with label 'deployment=${namespace}'"
                        echo "Failed to label node with error ${exmpl}"
                    }
                }
            }
        }
    }

    sleep 5
}