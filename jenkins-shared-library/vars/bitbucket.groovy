#!/usr/bin/env groovy
/*
* Name: Bitbucket Update
* Author: Albert Mugisha
* Date: 02-20-2018
*/



def call( params) {
    def endpoint = params.endpoint
    def method = params.method ?: "GET"
    def data = params.data ?: [:]
    def description = params.description ?: "Bitbucket Update"
    def username = params.username 
    def password = params.password
    def output = params.output ?: "true"


    def access_token = getBearerToken(username, password)
    def buildStatus = false
    int error_retries = 0

    URL url2 = endpoint.toURL()
    URLConnection connection2 = url2.openConnection()
    connection2.setDoOutput(true)
    connection2.setRequestMethod(method)
    connection2.setRequestProperty("Content-Type", "application/json")
    connection2.setRequestProperty ("Authorization", "Bearer "+access_token)
    if( data.size() != 0 || method == "POST") {
        connection2.getOutputStream().write(groovy.json.JsonOutput.toJson(data).getBytes("UTF-8"));
    }
    try {
        requestHeaders = ""
        resultCode = connection2.getResponseCode()
        InputStream inputStream = connection2.getInputStream()
        responseHeaders = ""
        returnArray = [
            "resultCode": resultCode,
            "message": inputStream.text
        ]
        inputStream.close()
    } catch( err) {
        println "Error is "+err
        responseHeaders = ""
        InputStream errorStream = connection2.getErrorStream()
        returnArray = [
            "resultCode": resultCode,
            "message": errorStream.text
        ]
  }

//   echo "Print output is ${output}"

  if ( output == "true" ) {
      println "Description: ${description}\n\n"\
          +"Request:\n"\
          +"\tUrl: ${endpoint}\n"\
          +"\tRequest Headers: ${requestHeaders}\n"\
          +"\tRequest Body: ${data}\n\n"\
          +"Response:\n"\
          +"\tResponse Code: ${returnArray['resultCode']}\n"\
          +"\tResponse Headers: ${responseHeaders}\n"\
          +"\tRespone Message: ${returnArray['message']}"
  }

  return returnArray

}

def getBearerToken(username, password) {
    authUrl = "https://bitbucket.org/site/oauth2/access_token"
    userpass = username + ":" + password;
    String basicAuth = "Basic " + javax.xml.bind.DatatypeConverter.printBase64Binary(userpass.getBytes());
    URL url = authUrl.toURL()
    def message = 'grant_type=client_credentials'
    URLConnection connection = url.openConnection()
    connection.setDoOutput(true)
    connection.setRequestMethod("POST")
    connection.setRequestProperty ("Authorization", basicAuth)
    connection.getOutputStream().write(message.getBytes("UTF-8"));
    InputStream inputStream = connection.getInputStream()
    def resultJSON = new groovy.json.JsonSlurper().parseText(inputStream.text)
    def access_token = resultJSON['access_token']
    inputStream.close()
    resultJSON = ""
    return access_token
}