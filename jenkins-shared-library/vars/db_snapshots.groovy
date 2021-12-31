#!/usr/bin/env groovy
/*
* Name: db_snapshots
* Author: Vinay Lakshmaiah
* Date: 11-17-2021
*/

import groovy.json.JsonSlurper

def call(String db_identifier, String dep_env){
    def now = new Date()
    def db_snapshot = "${db_identifier}-${now.format("yyyy-MM-dd-HHmmSS")}"
    withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: "aws_${dep_env}", variable: 'AWS_ACCESS_KEY_ID']]) {
        sh "aws rds create-db-snapshot --db-instance-identifier ${db_identifier} --db-snapshot-identifier cd-${db_snapshot} --region us-east-1"
        timeout(time: "600", unit: 'SECONDS') {
            def status = false
            while(status == false){
                def statusCommand = sh (script: "aws rds describe-db-snapshots --db-snapshot-identifier cd-${db_snapshot} --region us-east-1",returnStdout: true).trim()
                echo "${statusCommand}"
                def statusCommandJSON = stringToJSON str: statusCommand
                if(statusCommandJSON["DBSnapshots"][0]["Status"] == "available"){
                    status = true
                }
                statusCommandJSON = ""
                sleep 30
            }
        }
    }
}