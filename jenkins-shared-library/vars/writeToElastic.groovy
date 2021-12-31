#!/usr/bin/env groovy
/*
* Name: Post to Elasticsearch
* Author: Albert Mugisha
* Date: 03-07-2019
*/

import groovy.json.*



def call(params) {

    def environment = params.environment ?: "sandbox"
    def data = params.data ?: [:]
    def username = params.username
    def password = params.password
    def include_timestamp = params.include_timestamp ?: true
    def index = params.index ?: "logstash"
    def time_out = params.timeout ?: 60

    def userpass = username + ":" + password;
    String basicAuth = "Basic " + javax.xml.bind.DatatypeConverter.printBase64Binary(userpass.getBytes());

    def url_es_array = [
        "sandbox": "https://671f1b395ca2c86a99dca8c10d455c61.us-east-1.aws.found.io:9243/",
        "integration": "https://31754c2827db1d0e9452284ae87475c2.us-east-1.aws.found.io:9243",
        "staging": "https://671f1b395ca2c86a99dca8c10d455c61.us-east-1.aws.found.io:9243",
        "trial": "https://f81608447d8883d7a64e4e0997fd26eb.us-east-1.aws.found.io:9243",
        "private": "https://0cf5ffd8f5f9fa109239ad561ccf609b.us-east-1.aws.found.io:9243",
        "production": "https://e0eab4d45b71b561e24ea79af1875bb4.us-east-1.aws.found.io:9243"
    ]

    def now = new Date()
    if ( include_timestamp) {
        data["@timestamp"] = now 
        index = index+"-"+now.format("yyyy.MM.dd")
    }
    def jsonData = new JsonBuilder( data ).toPrettyString()

    def url = url_es_array[environment]+index+"/_doc"

    headers = [
        [
            name:  'Authorization',
            value: "${basicAuth}",
            maskValue: true
        ]
    ]

    

    def response = httpRequest(
        url: url,
        httpMode: 'POST',
        timeout: time_out,
        requestBody: jsonData,
        authorization: "${basicAuth}",
        customHeaders: headers,
        consoleLogResponseBody: true,
        contentType: 'APPLICATION_JSON'
    )


    print response

    return response
}