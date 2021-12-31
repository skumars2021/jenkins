#!/usr/bin/env groovy
/*
* Name: Mattermost Notification
* Author: Vinay Lakshmaiah
* Date: 10-18-2021
*/

import groovy.json.JsonSlurper


def call(params) {    
    def commit = params.commit
    def repo = params.repo
    def build_url = params.build_url
    def channel = params.channel ?: "cd-mop-staging"
    def branchname = params.branchname ?: "develop"

    def author = get_author(commit, repo)

    def MattermostNames = [
        "Dan Wilding": "dan.wilding",
        "Dinesh Subramanian": "dinesh.subramanian",
        "Kenneth Bingham": "kenneth.bingham",
        "Vinay Lakshmaiah": "vinay.lakshmaiah",
        "Mike Guthrie": "mike.guthrie",
        "Nick Pieros": "nick.pieros",
        "Russell Allen": "russell.allen",
        "Siva Sajja": "siva.sajja",
        "Evan Gertis": "evan.gertis",
        "Loren Fouts": "loren.love",
        "Randall Hoffman": "randall.hoffman",
        "Jens Alm": "jens.alm",
        "Harish Donepudi": "harish.donepudi",
        "Edward Moscardini": "edward.moscardini",
        "Ryan Galletto": "ryan.galletto",
        "Tod Burtchell": "tod.burtchell",
        "Steven Broderick": "steven.broderick",
        "Ed Thompson": "ed.thompson",
        "Nick Benton": "nick.benton"
    ]

    echo "Author is ${author}"
  
    username = ""
    try {
        if( author != null ) {
            username = MattermostNames["${author}"]
            if( username == null ) { username = "" } else { username = "@${username}"}
        }
    } catch(ex) {
        echo "Failed to get mattermost user with error ${ex}"
    }

    def notify_msg = "**Attn:** ${username}\n" +
                    "**Repo:** ${repo}\n" +
                    "**Failure Reason:** Build Failed for ${branchname} branch. Check build url to proceed\n"+
                    "*Links: **[BuildURL](${build_url})\n"

    if (branchname == "develop") {
        updateMattermost channel: "${channel}",
                color: "danger",
                message: notify_msg
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