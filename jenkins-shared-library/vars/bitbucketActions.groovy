/* Author: Albert Mugisha
* Used for Merges, Tags and Branch Closure
*/



def call( params ) {
    authUrl = "https://bitbucket.org/site/oauth2/access_token"
    apiUrl = "https://bitbucket.org/api"
    def source_branch = params.source_branch ?: ""
    def dest_branch = params.dest_branch ?: ""
    def action = params.action ?: ""
    def tag_name = params.tag_name ?: ""
    def repo = params.repo ?: ""
    switch(action){
        case "merge":
            branch_merge(source_branch, dest_branch, repo)
            break;
        case "tag":
            tagging(source_branch, tag_name, repo)
            break;
        case "close":
            // Check branch is not develop or master
            delete_branch(source_branch, repo)
            break;
    }
}

def branch_merge(source_branch, destination_branch, repo) {
  /*
  1. PR
  2. Approve
  3. Merge PR
  4. Get Build Status
  */
  //Raise PR
  def description = "Back merge ${source_branch} into ${destination_branch}"
  def prUrl = apiUrl+"/2.0/repositories/netfoundry/${repo}/pullrequests"
  def data = [
    "title": "Back merge ${source_branch} to ${destination_branch}",
    "source": [ "branch": ["name": "${source_branch}"]],
    "destination": [ "branch": ["name": "${destination_branch}"]]
  ]
  def prResult = bitbucket endpoint: prUrl, method: "POST",  data: data, description: "Commit Updates", username: BITBUCKET_AUTH_USR, password: BITBUCKET_AUTH_PSW, output: "true"
  if(prResult["resultCode"] >= 300) {
    if(prResult['message'].contains("There are no changes to be pulled")) {
      return
    }
    error("Failed to open PR for merge from ${source_branch} into ${destination_branch}")
  }

  //Approve PR
  def prResultJSON = new groovy.json.JsonSlurper().parseText(prResult["message"])
  def approveUrl = prResultJSON["links"]["approve"]["href"]
  description = "PR Approval: Back Merge ${source_branch} into ${destination_branch}"
  data = [:]
  bitbucket endpoint: approveUrl, method: "POST",  data: [:], description: "Commit Updates", username: BITBUCKET_ADMIN_USR, password: BITBUCKET_ADMIN_PSW, output: "false"

  //Merge PR
  def mergeUrl = prResultJSON["links"]["merge"]["href"]
  description = "PR Merge: Merge ${source_branch} into ${destination_branch}"
  data = [:]
  def mergeResult = bitbucket endpoint: mergeUrl, method: "POST",  data: [:], description: "Commit Updates", username: BITBUCKET_ADMIN_USR, password: BITBUCKET_ADMIN_PSW, output: "false"
  if(mergeResult["resultCode"] >= 300) {
    println mergeResult["resultCode"].getClass()
    println mergeResult['message']

    if(mergeResult['message'].contains("There are no changes to be pulled") || mergeResult['message'].contains("This pull request is already closed")) {
      return
    }
    error("Merge Failed: ${source_branch} into ${destination_branch}")
  }

  //Check Build Status
  prResultJSON = ""
  mergeResultJSON=""
}

def tagging(source_branch, tag_name, repo) {
    def getHashUrl = apiUrl+"/2.0/repositories/netfoundry/${repo}/commits/${source_branch}?pagelen=1"
    def hashResult = bitbucket endpoint: getHashUrl, method: "GET",  data: [:], description: "Commit Updates", username: BITBUCKET_AUTH_USR, password: BITBUCKET_AUTH_PSW, output: "true"
    def hashResultJSON = new groovy.json.JsonSlurper().parseText(hashResult["message"])
    commitHash = hashResultJSON["values"][0]["hash"]
    def tagURL = apiUrl+"/2.0/repositories/netfoundry/${repo}/refs/tags"
    def data = [
        "name" : "${tag_name}",
        "target" : [ "hash" : commitHash ]
    ]
    bitbucket endpoint: tagURL, method: "POST",  data: data, description: "Commit Updates", username: BITBUCKET_AUTH_USR, password: BITBUCKET_AUTH_PSW, output: "true"
    hashResultJSON = ""
    echo "Branch successfully tagged"
}

def delete_branch(source_branch, repo){
    if(params.source_branch == "develop" || params.source_branch == "master") {
        error "Cannot close develop or master branch"
    } else {
        //Close Branch
        def deleteUrl = apiUrl+"/2.0/repositories/netfoundry/${repo}/refs/branches/${source_branch}"
        bitbucket endpoint: deleteUrl, method: "DELETE",  data: [:], description: "Commit Updates", username: BITBUCKET_ADMIN_USR, password: BITBUCKET_ADMIN_PSW, output: "false"
    }
}