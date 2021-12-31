def call(approvers_emails, slackResponse){
    def INPUT_PARAMS = input message: 'Review TF Plan and choose how you want to proceed', ok: 'Next',
                    parameters: [
                    choice(name: 'runner_now', choices: 'Do not run Terraform\nApply Terraform\nAbort Pipeline', description: 'TF Decision')],
                    submitterParameter: 'approvedUser'
    labelledShell label: "Terraform approval results", script: """
        echo '${INPUT_PARAMS}'
        echo '${INPUT_PARAMS['approvedUser']}'
    """
    if(!approvers_emails.contains(INPUT_PARAMS['approvedUser'])) {
        try {
            error "${INPUT_PARAMS['approvedUser']}:- You are not among list of allowed approvers."
        } catch(e) {
            echo "Unathorized user is trying to approve pipeline."
        }
    } else {
        switch(INPUT_PARAMS['runner_now']) {
            case "Do not run Terraform":
                slackSend channel: slackResponse.threadId, color: "success", message: "skip terraform: approved by ${INPUT_PARAMS['approvedUser']}"
                echo "Not running Terraform Apply, proceeding"
                break
            case "Apply Terraform":
                slackSend channel: slackResponse.threadId, color: "success", message: "terraform apply: approved by ${INPUT_PARAMS['approvedUser']}"
                terraform_pr directory: "terraform", environment: "sandbox", execution: "apply"
            case "Abort Pipeline":
                slackSend channel: slackResponse.threadId, color: "failure", message: "abort pipeline: approved by ${INPUT_PARAMS['approvedUser']}"
                error "Pipeline aborted by user"
                break
            }
        }
}