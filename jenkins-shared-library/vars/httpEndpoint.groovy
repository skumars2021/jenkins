#!/usr/bin/env groovy
/*
* Name: Bitbucket Update
* Author: Albert Mugisha
* Date: 02-20-2018
*/

def call(params){
  def endpoints = params.endpoints
  def method = params.method ?: "GET"
  def data = params.data ?: [:]
  def stage_names = params.stage_names ?: []
  def description = params.description ?: "HTTP Request and Response"
  def access_token = params.access_token ?: null
  def parallelize = params.parallelize ?: false 
  def exit_on_error = params.exit_on_error ?: false
  def addittionalHeaders = params.addittionalHeaders ?: [:]
  def parallelHTTP = [:]
  def finalReturnArray = []
  verbose = params.verbose ?: "true"
  
  for ( int i =0; i<endpoints.size(); i++ ) {
    def endpoint = "${endpoints[i]}"
    if (stage_names.size() == 0 ) {
      stage_names = endpoints
    } 
    if( stage_names.size() != endpoints.size()) {
      error("Stage names array provided is not the same size as Endpoints array size!")
      sh "exit 1"
    }
    if( parallelize) {
      parallelHTTP[stage_names[i]] = { ->
        def returnArray = hitHTTP(endpoint, method, data, description, access_token, exit_on_error, addittionalHeaders)
        finalReturnArray.add(returnArray)
      }
    } else {
      def returnArray = hitHTTP(endpoint, method, data, description, access_token, exit_on_error, addittionalHeaders)
      finalReturnArray.add(returnArray)
    }
  }
  
  if(parallelize) {
    steps.parallel parallelHTTP
  }

  return finalReturnArray
}

def hitHTTP(endpoint, method, data, description, access_token, exit_on_error, addittionalHeaders) {
  def resultCode = 1000
  def requestHeaders = ""
  URL url = endpoint.toURL()
  URLConnection connection = url.openConnection()
  connection.setDoOutput(true)
  connection.setRequestMethod(method)
  connection.setRequestProperty("Content-Type", "application/json")
  connection.setRequestProperty("Cache-Control", "no-cache")
  if( access_token != null){
    connection.setRequestProperty ("Authorization", "Bearer "+access_token)
  }

  if(addittionalHeaders.size() != 0) {
    addittionalHeaders.each{header,value -> connection.setRequestProperty ("${header}", "${value}")}
  }

  if( data.size() != 0 || method == "POST") {
    connection.getOutputStream().write(groovy.json.JsonOutput.toJson(data).getBytes("UTF-8"));
  }


  try {
    resultCode = connection.getResponseCode()
    def inputStream = connection.getInputStream()
    responseHeaders = connection.getHeaderFields()
    returnArray = [
        "resultCode": resultCode,
        "message": inputStream.text
    ]
    inputStream.close()
    inputStream = ""
  } catch( err) {
    steps.echo "Error is "+err
    if( exit_on_error == true) {
      sh "exit 1"
    }
    def err_message = ""
    try {
      def errorStream = connection.getErrorStream()
      err_message = errorStream.text
      steps.echo "${errorStream.text}"
    } catch(err_msg) {
      err_message = err
    }
    responseHeaders = ""
    steps.echo "${err_message}"
    returnArray = [
          "resultCode": resultCode,
          "message": err_message
        ]
    errorStream = ""
  }

  try {
    def traceID = responseHeaders["X-B3-TraceId"]
    if( verbose == "true") {
      steps.echo "X-B3-TraceId: ${traceID}"
    }
  } catch(e) {
    if( verbose == "true") {
      echo "No X-B3-TraceId in this request"
    }
  }

  if( verbose == "true") {
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