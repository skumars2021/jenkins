#!/usr/bin/env groovy
/*
* Name: promotion_notify
* Author: Vinay Lakshmaiah
* Date: 11-19-2021
*/

import groovy.json.JsonSlurper

def call(String status, String userid, String commithash, String environment, String channel){
    def parameters_yaml = readYaml file: "pipelines/continous_deployment/conf/mattermost.yaml"
    def username = ""
    if(environment == "production"){
        username = parameters_yaml['MattermostEmail'][userid]

    }else {
        username = parameters_yaml["MattermostNames"][userid]
    }
    if( username == null ) { username = "" } else { username = "@${username}"}
    def notify_msg = "Attn: ${username}\n"+
            "**Repo:** ${params.REPO}\n" +
            "**Commit Hash:** ${commithash}\n" +
            "**Result:** Promotion to ${environment} started. Please check link for details\n"+
            "**Links:** [BuildURL](${RUN_DISPLAY_URL})\n"

    updateMattermost channel: "${channel}",
                color: "good",
                message: notify_msg
}