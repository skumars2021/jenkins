
import groovy.json.*


def call(params) {

    if(!params.query) {
        throw new Exception('query is a required parameter ')
    }

    def size = params.size ?: 10
    def sort_oder = params.sort_order ?: "desc"
    def start_time = params.start_time ?: "now-24h"
    def end_time = params.end_time ?: "now"
    def index = params.index ?: "logstash"
    def query = params.query
    def environment = params.environment ?: "sandbox"

    def url_es_array = [
        "sandbox": "https://671f1b395ca2c86a99dca8c10d455c61.us-east-1.aws.found.io:9243/",
        "integration": "https://31754c2827db1d0e9452284ae87475c2.us-east-1.aws.found.io:9243/",
        "staging": "https://671f1b395ca2c86a99dca8c10d455c61.us-east-1.aws.found.io:9243/",
        "trial": "https://f81608447d8883d7a64e4e0997fd26eb.us-east-1.aws.found.io:9243/",
        "private": "https://0cf5ffd8f5f9fa109239ad561ccf609b.us-east-1.aws.found.io:9243/",
        "production": "https://e0eab4d45b71b561e24ea79af1875bb4.us-east-1.aws.found.io:9243/"
    ]

    def query_string = '{\n' + 
            ' "version": true,\n' +
            ' "size": '+size+',\n'  +
            ' "sort": [\n' +
                ' {\n' +
                ' "@timestamp": {\n' +
                    ' "order": "'+sort_oder+'",\n' +
                    ' "unmapped_type": "boolean"\n' +
                ' }\n' +
                ' }\n' +
            ' ],\n' +
            ' "docvalue_fields": [\n' +
                ' {\n' +
                ' "field": "@timestamp",\n' +
                ' "format": "date_time"\n' +
                ' }\n' +
            ' ],\n' +
            ' "query": {\n' +
                ' "bool": {\n' +
                ' "must": [\n' +
                    ' {\n' +
                    ' "match_all": {}\n' +
                    ' },\n' +
                    ' {\n' +
                    ' "query_string": {\n' +
                        ' "query": "'+query+'",\n' +
                        ' "analyze_wildcard": true,\n' +
                        ' "default_field": "*"\n' +
                    ' }\n' +
                    ' },\n' +
                    ' {\n' +
                    ' "range": {\n' +
                        ' "@timestamp": {\n' +
                        ' "gte": "'+start_time+'",\n' +
                        ' "lte": "'+end_time+'",\n' +
                        ' "format": "epoch_millis"\n' +
                        ' }\n' +
                    ' }\n' +
                    ' }\n' +
                ' ]\n' +
                ' }' +
            ' }' +
            ' }'

    // println query_string

    withCredentials([usernamePassword(credentialsId: 'jenkins_es_reader', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
        def userpass = USERNAME + ":" + PASSWORD
        String basicAuth = "Basic " + javax.xml.bind.DatatypeConverter.printBase64Binary(userpass.getBytes());

        headers = [
                [
                        name:  'Authorization',
                        value: basicAuth,
                        maskValue: true
                ]
        ]

        def es = url_es_array[environment]+ index +'-*/_search/'

        def response = httpRequest(
                    url: es,
                    requestBody: query_string,
                    httpMode: 'POST',
                    timeout: 60,
                    customHeaders: headers,
                    consoleLogResponseBody: true,
                    contentType: 'APPLICATION_JSON'
            )

        // println response

        return response.content

    }


}
