#!/usr/bin/env groovy
/*
* Name: transformations
* Author: Vinay Lakshmaiah
* Date: 11-30-2021
*/

import groovy.json.JsonSlurper
import org.apache.commons.lang3.StringUtils;
import hudson.model.*

def call(parameters_yaml, service, env_variable, value, namespace, environment){
    value = value.replaceAll('auth.mop.local','auth-service')

    // redis overrides - yuck
    value = value.replaceAll('nfn-internal-env-rsc.iy4pix.ng.0001.use1.cache.amazonaws.com','eks-cd-staging.iy4pix.ng.0001.use1.cache.amazonaws.com')
    value = value.replaceAll('nfn-internal-env-rsc.8wqguh.ng.0001.use1.cache.amazonaws.com','eks-cd-production.8wqguh.ng.0001.use1.cache.amazonaws.com')

    // rabbitmq overrides - yuck
    value = value.replaceAll('amqps://b-40231d97-d2cc-4234-b127-d125e32d4a53.mq.us-east-1.amazonaws.com:5671','amqps://b-e9ebbe91-aa67-44cb-b7b7-1e207f5d2c7b.mq.us-east-1.amazonaws.com:5671')
    value = value.replaceAll('amqps://b-a6a2d94c-2491-4c4b-963f-145bdff460af.mq.us-east-1.amazonaws.com:5671','amqps://b-3582d6ec-7219-41cd-9d30-22f0cede4548.mq.us-east-1.amazonaws.com:5671')

    def return_val = value
    def parameters = parameters_yaml["Services"]["${service}"]["env_replacements"]
    
    try {
        if(value.contains(".${environment}.netfoundry.io") || value.contains(".mop.local")) {
            return_val = value.replaceAll(".${environment}.netfoundry.io","-service")
            return_val = return_val.replaceAll("https", "http")
            return_val = return_val.replaceAll("-service.mop.local","-service")
            return_val = return_val.replaceAll(".mop.local","-service")
        }
    } catch(e) {
        // echo "Error is ${e}"
        return_val = value
    }
    
    if( parameters != null) {
        if( parameters["${env_variable}"] != null ) {
            return_val = parameters["${env_variable}"]
        }
        // Production
        // if( ( service == "data" || service == "infra" ) && environment == "production"){
        //     def parameters_prod = parameters_yaml["Services"]["${service}"]["prod_env_replacements"]
        //     if(parameters_prod["${env_variable}"] != null) {
        //         return_val = parameters_prod["${env_variable}"]
        //     }
        // }
    }
    if( value.contains("gateway.${environment}.netfoundry.io")){
        return_val = "https://gateway-${namespace}.${environment}.netfoundry.io"
    }
    if(environment == "production"){
        return_val = return_val.replaceAll("tf-mop-events", "tf-mop-events-eks-cd")
        return_val = return_val.replaceAll("tf-organization-events", "tf-organization-events-eks-cd")
        return_val = return_val.replaceAll("tf-mop-alerts", "tf-mop-alerts-eks-cd")
    }
    return return_val
}