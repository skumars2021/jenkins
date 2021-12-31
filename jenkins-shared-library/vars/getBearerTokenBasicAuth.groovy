#!/usr/bin/env groovy
/*
* Name: Bitbucket Update
* Author: Albert Mugisha
* Date: 02-20-2018
*/



def call(params) {
    def authUrl = params.authUrl
    def username = params.username
    def password = params.password
    def data = params.data ?: [:]
    def userpass = username + ":" + password
    String basicAuth = "Basic " + javax.xml.bind.DatatypeConverter.printBase64Binary(userpass.getBytes());
    URL url = authUrl.toURL()
    URLConnection connection = url.openConnection()
    connection.setDoOutput(true)
    connection.setRequestMethod("POST")
    connection.setRequestProperty ("Authorization", basicAuth)
    if( data.size() != 0 ){
        connection.getOutputStream().write(data.getBytes("UTF-8"));
    }
    InputStream inputStream = connection.getInputStream()
    def resultJSON = new groovy.json.JsonSlurper().parseText(inputStream.text)
    def access_token = resultJSON['access_token']
    inputStream.close()
    resultJSON = ""
    return access_token
}

