#!/usr/bin/env groovy
/*
* Name: check_service_availability
* Author: Vinay Lakshmaiah
* Date: 11-25-2021
*/

import groovy.json.JsonSlurper

def call(service, time_out, command) {
    def status = false
    timeout(time: time_out, unit: 'SECONDS') {
        while(status == false) {
            try {
                labelledShell label: "Checking if ${service} is reachable", script: """
                    ${command}
                """
                status = true
            } catch(e) {
                labelledShell label: "Service ${service} is not reachable, will try again in 5 sec", script: ""
            }
            sleep 5
        }
    }
}