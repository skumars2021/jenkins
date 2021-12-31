#!/usr/bin/env groovy
/*
* Name: has_database
* Author: Vinay Lakshmaiah
* Date: 11-23-2021
*/

import groovy.json.JsonSlurper

def call(String repo){
    def parameters_yaml = readYaml file: "pipelines/continous_deployment/conf/services.yaml"
    def db_services = parameters_yaml["Services"]
    if( repo == "network"){
        return "nfapidb"
    }
    def service_lookup = repo
    if( service_lookup == "api-gateway"){
        service_lookup = "gateway"
    }
    if(db_services["${service_lookup}"].containsKey("database")) {
        def database = db_services["${repo}"]["database"]
        echo "Database is ${database}"
        return database
    } else {
        echo "Database is null"
        return null
    }
}