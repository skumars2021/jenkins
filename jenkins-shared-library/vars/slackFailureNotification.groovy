#!/usr/bin/env groovy
/*
* Name: Slack Notification
* Author: Albert Mugisha
* Date: 04-22-2019
*/

import groovy.json.JsonSlurper


def call(params) {    
    def commit = params.commit
    def repo = params.repo
    def build_url = params.build_url
    def channel = params.channel ?: "continous-deployments-staging"
    def branchname = params.branchname ?: "develop"

    def author = get_author(commit, repo)

    def slackAccounts = [
        "Albert Mugisha": "UCG7GLZ5W",
        "Dan Wilding": "U5UTGLBFV",
        "Dinesh Subramanian": "UJQ0QN5FD",
        "Joseph Lombardo": "U3PC7BL9X",
        "Kenneth Bingham": "U6Y8UNG59",
        "Mary D’couto": "UARAA3BEW",
        "Mike Guthrie": "U6U329WQZ",
        "Nick Pieros": "U9P6CEVT7",
        "Olaf Luetkehoelter": "U315V9ZDY",
        "Russell Allen": "U8EAZTW56",
        "Siva Sajja": "UHS1DRK7V",
        "Vinay Lakshmaiah": "U6ZG28L69",
        "Evan Gertis": "URZQGTX89",
        "Randall Hoffman": "UVBBACHNV",
        "Loren Fouts": "URZQGTX89",
        "Jens Alm": "U013PUM46CQ"
    ]

    echo "Author is ${author}"
  
    username = ""
    try {
        if( author != null ) {
            username = slackAccounts["${author}"]
            if( username == null ) { username = "" } else { username = "<@${username}>"}
        }
    } catch(ex) {
        echo "Failed to get slack user with error ${ex}"
    }

    def slack_msg = "*Attn:* ${username}\n" +
                    "*Repo:* ${repo}\n" +
                    "*BuildURL:* ${build_url}\n"+
                    "*Failure Reason:* Build Failed for ${branchname} branch. Check build url to proceed\n"

    if ( GIT_BRANCH == "develop") {
        slackSend channel: "${channel}",
        color: "danger",
        message: slack_msg
    }
}

def get_author(commit, repo) {
    try {
        commitUrl = "https://bitbucket.org/api/2.0/repositories/netfoundry/${repo}/commit"
        withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: "bitbucket_auth", usernameVariable: 'BB_USER', passwordVariable: 'BB_PASS']]) {
            def commit_details_raw = bitbucket username: BB_USER, password: BB_PASS, endpoint: "${commitUrl}/${commit}"
            def commit_details = new groovy.json.JsonSlurperClassic().parseText(commit_details_raw['message'])
            def author_fname = commit_details['author']['raw'].split(" ")[0]
            def author_lname = commit_details['author']['raw'].split(" ")[1]
            return "${author_fname} ${author_lname}"
        }
    } catch(e) {
        echo "Error in getting author is ${e}"
        return null
    }
 }