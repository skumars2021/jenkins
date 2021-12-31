


import groovy.json.*



def call(params) {

    if(!params.buildNumber) {
        throw new Exception('Build number is a required parameter to use library "getBuildQualityResults" ')
    }

    params.threshold = params.threshold ? params.threshold : 97
    params.minimum = params.minimum ? params.minimum : 150

    passed = evaluateBuild(params.buildNumber, params.threshold, params.minimum)


    return passed

}



def evaluateBuild(buildNumber, threshold, minimum) {

    query = '{\n' +
            '  "aggs": {\n' +
            '    "Build": {\n' +
            '      "terms": {\n' +
            '        "field": "Build",\n' +
            '        "size": 2\n' +
            '      },\n' +
            '      "aggs": {\n' +
            '        "StepStatus": {\n' +
            '          "terms": {\n' +
            '            "field": "StepStatus",\n' +
            '            "size": 2\n' +
            '          }\n' +
            '        }\n' +
            '      }\n' +
            '    }\n' +
            '  },\n' +
            '  "size": 0,\n' +
            '  "docvalue_fields": [\n' +
            '    {\n' +
            '      "field": "@timestamp",\n' +
            '      "format": "date_time"\n' +
            '    }\n' +
            '  ],\n' +
            '  "query": {\n' +
            '    "bool": {\n' +
            '      "must": [\n' +
            '        {\n' +
            '          "match_all": {}\n' +
            '        },\n' +
            '        {\n' +
            '          "range": {\n' +
            '            "@timestamp": {\n' +
            '              "gte": "now-24h",\n' +
            '              "lte": "now",\n' +
            '              "format": "epoch_millis"\n' +
            '            }\n' +
            '          }\n' +
            '        },\n' +
            '        {\n' +
            '          "match_phrase": {\n' +
            '            "Build": {\n' +
            '              "query": "'+buildNumber+'"\n' +
            // '              "query": "56"\n' +
            '            }\n' +
            '          }\n' +
            '        }\n' +
            '      ]\n' +
            '    }\n' +
            '  }\n' +
            '}'


    response = elasticQuery('dockersoap-*', query, true)


    try {
        jsonSlurper = new groovy.json.JsonSlurper()
        res = jsonSlurper.parseText(response.content)

        passed = 0
        failed = 0
        total = res.hits.total

        res.aggregations.Build.buckets[0].StepStatus.buckets.each {

            if(it.key == "OK") {
                passed = it.doc_count
            }
            if(it.key == "FAILED") {
                failed = it.doc_count
            }
        }
    } catch(err) {
        println "ERROR evaluating Test Results: "+err
        // currentBuild.rawBuild.@result = hudson.model.Result.FAILURE
        return false
    }

    score = (( passed / total) * 100 as double)
    println "================================================================="
    println "Total Steps Run: "+total
    println "Steps Passed: "+passed
    println "Steps Failed: "+failed
    println "=== BUILD SCORE: "+score.round(2)+"% ===="

    if (total < minimum) {
        println "Not enough tests have been run, failing... Minimum tests ${minimum}"
        // currentBuild.rawBuild.@result = hudson.model.Result.FAILURE
        // garbage cleanup because jenkins is stupid about some things
        res = ""
        total = ""
        passed = ""
        failed = ""
        score = ""
        response = ""
        jsonSlurper = ""
        return false
    }

    else if(score < threshold) {
        println "Build score is below threshold of "+threshold+"%, failing the build...."
        // currentBuild.rawBuild.@result = hudson.model.Result.FAILURE
        // garbage cleanup because jenkins is stupid about some things
        res = ""
        total = ""
        passed = ""
        failed = ""
        score = ""
        response = ""
        jsonSlurper = ""
        return false

    } else {
        // currentBuild.rawBuild.@result = hudson.model.Result.SUCCESS

    }

    currentBuild.description = currentBuild.description+ " / StepsVsMin: ${total}/${minimum} / SCORE: "+score.round(2)+"%"


    // garbage cleanup because jenkins is stupid about some things
    res = ""
    total = ""
    passed = ""
    failed = ""
    score = ""
    response = ""
    jsonSlurper = ""

    return true


}





def elasticQuery(index, query, logBody) {


    withCredentials([usernamePassword(credentialsId: 'jenkins_es_reader', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {

        def userpass = USERNAME + ":" + PASSWORD
        println "ES CRED: "+userpass
        String basicAuth = "Basic " + javax.xml.bind.DatatypeConverter.printBase64Binary(userpass.getBytes());

        headers = [
                [
                        name:  'Authorization',
                        value: basicAuth,
                        maskValue: true
                ]
        ]

        es = 'https://671f1b395ca2c86a99dca8c10d455c61.us-east-1.aws.found.io:9243/'+ index +'/_search/'

        try {

            def response = httpRequest(
                    url: es,
                    requestBody: query,
                    httpMode: 'POST',
                    timeout: 60,
                    customHeaders: headers,
                    consoleLogResponseBody: logBody,
                    contentType: 'APPLICATION_JSON'
            )

            return response

        } catch(err) {
            println "Something went wrong: $err"
        }
    }


}