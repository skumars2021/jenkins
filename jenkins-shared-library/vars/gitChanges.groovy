def call(bb_request_raw){
    terraform_changes = false
    flywaymigration_changes = false
    bb_request = new groovy.json.JsonSlurperClassic().parseText(bb_request_raw)
    REPO = bb_request.pullrequest.source.repository.full_name.split("/")[1]
    BB_COMMENTS_URL="${bb_request.pullrequest.links.comments.href}"
    BB_APPROVE_URL="${bb_request.pullrequest.links.approve.href}"
    BB_DECLINE_URL="${bb_request.pullrequest.links.decline.href}"
    BB_BRANCH="${bb_request.pullrequest.source.branch.name}"
    BB_DESTINATION_BRANCH="${bb_request.pullrequest.destination.branch.name}"
    COMMIT_HASH = "${bb_request.pullrequest.source.commit.hash}"
    TITLE = "${bb_request.pullrequest.rendered.title.raw}"
    NAMESPACE = BB_BRANCH.replaceAll("/","-").toLowerCase()
    NAMESPACE = "${REPO}-${NAMESPACE}"
    NAMESPACE = NAMESPACE.replaceAll("_","-")
    NAMESPACE = NAMESPACE.replaceAll("\\.","-")
    labelledShell label: "Git diff between branch ${BB_BRANCH} and develop", script: """
        sleep 5
        git diff --name-only 'HEAD^'
        git diff --name-only origin/develop...
    """
    def CHANGES = sh (script: "git diff --name-only origin/develop...",label:"Saving Git diff to a variable",returnStdout: true).trim()
    def CHANGED_ARRAY = CHANGES.split("\n")
    
    for( String chk_array: CHANGED_ARRAY) {
        if( chk_array.contains("terraform")) {
            labelledShell label: "Terraform Changes Detected.", script: """
                echo 'Terraform changes detected. Pause execution till approval'
                echo '${chk_array}'
            """
            terraform_changes = true
        }
        if( chk_array.contains("migration")) {
            labelledShell label: "Database Migration Changes Detected.", script: """
                echo 'DB Migration changes detected'
                echo '${chk_array}'
            """
            flywaymigration_changes = true
        }
    }
    return [terraform : "${terraform_changes}", flyway : "${flywaymigration_changes}", bb_branch : "${BB_BRANCH}", bb_title : "${TITLE}", repo : "${REPO}",  namespace : "${NAMESPACE}"]
}