#!/usr/bin/env groovy
/*
* Name: deploy_centos
* Author: Vinay Lakshmaiah
* Date: 11-24-2021
*/

import groovy.json.JsonSlurper
import org.apache.commons.lang3.StringUtils;
import hudson.model.*

def call(environment, namespace, cluster_name, HELM_TIMEOUT, helm_verbose_level){
    def CENTOS_CONTAINER = ""
    def account_id = nfAWSAccounts account_name: "${environment}"
    dir("nf-mop-kubernetes") {
        def helm_release = "centos-${namespace}"
        if(helm_release.size() > 50) {
            helm_release = helm_release.substring(0,50)+"a"
        }
        withAWS(credentials: 'jenkins-central-mgt', region: 'us-east-1', role:'jenkins-central-mgt',  roleAccount: "${account_id}") {
            labelledShell label: "Create centos container in namespace ${namespace}", script: """
                kubectl config use-context ${cluster_name} > /dev/null; helm delete ${helm_release} --namespace ${namespace} || true
                kubectl config use-context ${cluster_name} > /dev/null; helm install ${helm_release} centos-image --namespace ${namespace} --set namespace=${namespace} --wait --timeout ${HELM_TIMEOUT} -v=${helm_verbose_level}
            """
            CENTOS_CONTAINER=sh (script: "kubectl config use-context ${cluster_name} > /dev/null; kubectl get pods --namespace=${namespace} | grep Running| grep 'centos-image' | cut -d' ' -f1",returnStdout: true).trim()
            labelledShell label: "Centos Image - ${CENTOS_CONTAINER}", script: ""
        }
    }
    return CENTOS_CONTAINER
}