#!/usr/bin/env groovy
/*
* Name: terraform
* Author: Vinay Lakshmaiah
* Date: 11-19-2021
*/

import groovy.json.JsonSlurper

def call(String repo, String environment, String commit_hash, String startedUserId, String channel) {
    def tfversion = "0.11.15"
    def workspace = (environment == "production") ? "nf-prod-int": "nf-${environment}-int"
    def command_plan = "tfenv use ${tfversion}; "+
                "terraform workspace list; "+
                "terraform -v; "+
                "terraform plan"
    def command_apply = "tfenv use ${tfversion}; "+
                "terraform workspace list; "+
                "terraform apply --auto-approve"
    def parameters_yaml = readYaml file: "../pipelines/continous_deployment/conf/mattermost.yaml"
    
    if (repo == "network") {
        def buildTF = [ "data", "edge", "engine", "infra"]
        for( String build: buildTF) {
            dir("${build}/terraform") {
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: "ba4f105d-6e90-4b8a-ad8e-6e31ce4c339b", variable: 'AWS_ACCESS_KEY_ID']]) {
                    echo "Terraform for ${build}"
                    sh(script: "rm -rf .terraform; tfenv use ${tfversion}; terraform init;  terraform workspace select ${workspace}; terraform init; tfenv use ${tfversion}; source ../../auth-env.sh && ${command_plan}", label: "Terraform Plan")
                    if(environment == "production"){
                        def username= parameters_yaml['MattermostEmail'][startedUserId]
                        if(username == null) {
                            def author1 = fetch_author(repo, commit_hash)
                            username= parameters_yaml['MattermostNames'][author1]
                        }
                        // try{
                        //     def buildUser = currentBuild.rawBuild.getCause(Cause.UserIdCause).getUserId()
                        // }catch(ex){
                        //     def buildUser = "automated_start"
                        // }
                        def notify_msg = "Attn: @${username}\n"+
                            // "Started By: @${buildUser}\n" +
                            "**Repo:** ${repo}\n" +
                            "**Commit Hash:** ${commit_hash}\n" +
                            "**Result:** Terraform Changes detected. Please review and approve\n"+
                            "**Links:** [BuildURL](${RUN_DISPLAY_URL})\n"
                        updateMattermost channel: "${channel}",
                                    color: "good",
                                    message: notify_msg
                        def INPUT_PARAMS = input message: 'Please review Terraform Changes, and Click Approved if Ok', ok: 'Next',
                            parameters: [
                                booleanParam(name: 'run_terraform', defaultValue: true, description: 'Apply Terraform')
                            ], submitterParameter: 'approvedUser'
                        
                        if(INPUT_PARAMS['run_terraform'] == true){
                            sh(script: "rm -rf .terraform; tfenv use ${tfversion}; terraform init;  terraform workspace select ${workspace}; terraform init; tfenv use ${tfversion}; source ../../auth-env.sh && ${command_apply}", label: "Terraform Apply")
                        }
                    } else {
                        sh(script: "rm -rf .terraform; tfenv use ${tfversion}; terraform init;  terraform workspace select ${workspace}; terraform init; tfenv use ${tfversion}; source ../../auth-env.sh && ${command_apply}", label: "Terraform Apply")
                    }
                }
            }
        }
    }else {
        dir("terraform") {
            withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: "ba4f105d-6e90-4b8a-ad8e-6e31ce4c339b", variable: 'AWS_ACCESS_KEY_ID']]) {
                sh(script: "rm -rf .terraform; tfenv use ${tfversion}; terraform init;  terraform workspace select ${workspace}; terraform init; tfenv use ${tfversion}; source ../auth-env.sh && ${command_plan}", label: "Terraform Plan")
                if(environment == "production"){
                    def username1 = parameters_yaml['MattermostEmail'][startedUserId]
                    // try{
                    //   def buildUser = currentBuild.rawBuild.getCause(Cause.UserIdCause).getUserId()
                    // }catch(ex){
                    //     def buildUser = "automated_start"
                    // }
//                    def notify_msg1 = "Attn: @${username1}\n"+
//                            // "Started By: @${buildUser}\n" +
//                            "**Repo:** ${repo}\n" +
//                            "**Commit Hash:** ${commit_hash}\n" +
//                            "**Result:** Terraform Changes detected. Please review and approve\n"+
//                            "**Links:** [BuildURL](${RUN_DISPLAY_URL})\n"
//                    updateMattermost channel: "${channel}",
//                                    color: "good",
//                                    message: notify_msg1
                    def INPUT_PARAMS = input message: 'Please review Terraform Changes, and Click Approved if Ok', ok: 'Next',
                        parameters: [
                            booleanParam(name: 'run_terraform', defaultValue: true, description: 'Apply Terraform')
                        ], submitterParameter: 'approvedUser'
                    if(INPUT_PARAMS['run_terraform'] == true){
                        sh(script: "rm -rf .terraform; tfenv use ${tfversion}; terraform init;  terraform workspace select ${workspace}; terraform init; tfenv use ${tfversion}; source ../auth-env.sh && ${command_apply}", label: "Terraform Apply")
                    }
                } else {
                    sh(script: "rm -rf .terraform; tfenv use ${tfversion}; terraform init;  terraform workspace select ${workspace}; terraform init; tfenv use ${tfversion}; source ../auth-env.sh && ${command_apply}", label: "Terraform Apply")
                }
            }
        }
    }
}