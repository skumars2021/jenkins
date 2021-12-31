#!/usr/bin/env groovy
/*
* Name: Update Mattermost
* Author: Vinay Lakshmaiah
* Date: 09-23-2021
*/

def call(params){
  def color = params.color ?: "good"
  def message = params.message ?: ""
  def channel = params.channel ?: "test-automation"
  def addittionalHeaders = params.addittionalHeaders ?: [:]
  
  sendToMattermost(color, message, channel, addittionalHeaders)
}

def sendToMattermost(color, message, channel, addittionalHeaders) {
  def resultCode = 1000

  withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: "mattermost_webhook", usernameVariable: 'SENDER_NAME', passwordVariable: 'MATTERMOST_WEBHOOK']]) {
    sender_name = SENDER_NAME
    mattermost_webhook = MATTERMOST_WEBHOOK
  }

  def attachment = [
        "color": color,
        "text": message
      ]
  def data = [
        "channel": channel,
				"username": sender_name,
        "attachments": [attachment]
      ]

  URL url = mattermost_webhook.toURL()
  URLConnection connection = url.openConnection()
  connection.setDoOutput(true)
  connection.setRequestMethod("POST")
  connection.setRequestProperty("Content-Type", "application/json")
  connection.setRequestProperty("Cache-Control", "no-cache")

  if(addittionalHeaders.size() != 0) {
    addittionalHeaders.each{header,value -> connection.setRequestProperty ("${header}", "${value}")}
  }

  connection.getOutputStream().write(groovy.json.JsonOutput.toJson(data).getBytes("UTF-8"));
  
  try {
    resultCode = connection.getResponseCode()
    def inputStream = connection.getInputStream()
    inputStream.close()
    inputStream = ""
  } catch(err) {
    steps.echo "Error is "+err
    def err_message = ""
    try {
      def errorStream = connection.getErrorStream()
      err_message = errorStream.text
      steps.echo "${errorStream.text}"
    } catch(err_msg) {
      err_message = err
    }
    steps.echo "${err_message}"
    errorStream = ""
  }
}