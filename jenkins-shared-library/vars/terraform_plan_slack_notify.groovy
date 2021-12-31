
def call(bb_request_raw){
    bb_request = new groovy.json.JsonSlurperClassic().parseText(bb_request_raw)
    // TF_APPROVERS= [ SLACK_MEMBER_ID : JENKINS_USER_ID]
    TF_APPROVERS=[ 
        "U5UTGLBFV": "dan.wilding@netfoundry.io",
        "U6U329WQZ": "mike.guthrie@netfoundry.io",
        "UJQ0QN5FD": "dinesh.subramanian@netfoundry.io",
        "UARAA3BEW": "mary.dcouto@netfoundry.io",
        "U6Y8UNG59": "kenneth.bingham@netfoundry.io",
        "UVBBACHNV": "randall.hoffman@netfoundry.io",
        "U021E2X2FPH": "nick.benton@netfoundry.io"
    ]
    // Send Slack to TF Approvers
    def approvers = ""
    def approvers_emails = []
    TF_APPROVERS.each {k,v ->
        approvers = approvers+ "<@${k}> "
        approvers_emails.add(v)
    }
    terraform_pr directory: "terraform", environment: "sandbox", execution: "plan"

    def slack_msg = "Attn: ${approvers} \n"+
                    "*PR #${bb_request.pullrequest.id} [${bb_request.pullrequest.rendered.title.raw}] has Terraform changes and needs to be approved.* Please click Jenkins link below to approve/deny Terraform run for PR\n"+
                    "*Author*: ${bb_request.actor.display_name}\n"+
                    "*Repo:* ${bb_request.pullrequest.source.repository.full_name.split("/")[1]}\n" +
                    "*Branch:* ${bb_request.pullrequest.source.branch.name}\n" +
                    "*PR Link:* ${bb_request.pullrequest.links.html.href}\n"+
                    "*Jenkins Approver Link:* ${RUN_DISPLAY_URL}\n"

    slackResponse = slackSend channel: "pr-builds",
                    color: "success",
                    message: slack_msg
    return [approvers_emails, slackResponse]
}