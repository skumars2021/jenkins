#!/usr/bin/env groovy
/*
* Name: Find Free Node
* Author: Albert Mugisha
* Date: 02-20-2018
*/

def call( params ) {
    def project = params.project
    def slavepool = params.slavepool
    def SLEEP = params.sleep_time
    def result = "none"
    def run_result = startStopNode(["node": "none", "action": "START", "slavepool": slavepool, "project": project] )
    steps.println run_result
    status = run_result.split(",")[0]
    if (status == "started") {
        result = run_result.split(",")[1]
        steps.println "Slave chosen is ${result}"
    }
    while( status == "non-available") {
        steps.println "No slave available - sleeping for ${SLEEP} sec"
        sleep SLEEP
        run_result = startStopNode(["node": "none", "action": "START", "slavepool": slavepool, "project": project])
        status = run_result.split(",")[0]
        if (status != "non-available") {
            result = run_result.split(",")[1]
            steps.println "Slave chosen is ${result}"
        }
    }

    return result
}