// Update BB Status
import org.apache.commons.codec.digest.DigestUtils;


def call( params ) {
    def repo = params.repo
    def status = params.status
    def BRANCH_NAME = params.BRANCH_NAME
    def GIT_COMMIT = params.GIT_COMMIT
    def pr_check = params.PR_CHECK ?: "true"
    def build_name = params.BUILD_NAME ?: "Jenkins Build"
    pr_open = false
    IGNORE_TITLES = [ "WIP", "URGENT", "MERGE RELEASE", "BACK MERGE", "BYPASS"]
    if(repo == "common-util-framework" || repo == "tracing-framework" || repo == "elasticsearch-client" || repo == "ziti-api-client" || repo == "dart-api-client" || repo == "oss" || repo == "OSS" || repo == "process-framework"){
        echo "Am in a library, no PR override"
    } else {
        if( pr_check == "true"){
            try {
                def prUrl = 'https://bitbucket.org/api/2.0/repositories/netfoundry/'+repo+'/pullrequests?q=state="open"'
                def prResultRaw = bitbucket endpoint: prUrl, method: "GET",  data: [], description: "Get PRs", username: BITBUCKET_AUTH_USR, password: BITBUCKET_AUTH_PSW, output: "false"
                def prResult = new groovy.json.JsonSlurperClassic().parseText(prResultRaw['message']) 
                for ( int j = 0; j < prResult['values'].size(); j++ ) {
                    def branch = prResult['values'][j]["source"]["branch"]["name"]
                    def dest_branch = prResult['values'][j]["destination"]["branch"]["name"]
                    if ( branch == BRANCH_NAME && dest_branch == "develop") {
                        pr_open = true
                        for(String ignoreStr: IGNORE_TITLES) {
                            if(prResult['values'][j]["title"].toUpperCase().startsWith("${ignoreStr}")) {
                                pr_open = false
                            }
                        }
                    }
                }
            } catch(e) {
                echo "Unable to read PR information"
            }

            if( pr_open == true) {
                echo "Not updating Bitbucket because there is an open PR"
                return
            }
        }
    }
    
    
    


    def commitUrl = "https://bitbucket.org/api/2.0/repositories/netfoundry/${repo}/commit/"
    def bitbucket_key = DigestUtils.md5Hex(BRANCH_NAME)
    def bitbucket_error_retries = 10
    String updateUrl = commitUrl+GIT_COMMIT+"/statuses/build"
    def message = [
        "key" : bitbucket_key ,
        "state" : status ,
        "name" : build_name ,
        "url" : RUN_DISPLAY_URL,
        "description" : "Jenkins Builds"
      ]
    def result = bitbucket endpoint: updateUrl, method: "POST",  data: message, description: "Commit Updates", username: BITBUCKET_AUTH_USR, password: BITBUCKET_AUTH_PSW, output: "false"
    def buildStatus = false
    int error_retries = 0

    while ( !buildStatus ) {
        try {
            def statusUrl = commitUrl+GIT_COMMIT+"/statuses/build/"+bitbucket_key
            def statusResult = bitbucket endpoint: statusUrl, method: "GET",  data: [], description: "Commit Update", username: BITBUCKET_AUTH_USR, password: BITBUCKET_AUTH_PSW, output: "false"
            def hashResultJSON = new groovy.json.JsonSlurper().parseText(statusResult["message"])

            if ( hashResultJSON["state"] == status ) {
                buildStatus = true
                // println "Bitbucket sucessfully updated for ${GIT_COMMIT} with ${status}"
            } else {
                // println "Bitbucket update not complete, will sleep for 5sec and then try again"
                buildStatus = false
                sleep 5
            }
        } catch( err) {
            println "Got an error ${err} - will try again in 5 sec, Try# ${error_retries}"
            if ( error_retries >= bitbucket_error_retries ) {
                buildStatus = true
            }
            error_retries = error_retries + 1
            sleep 5
        }
    }
}