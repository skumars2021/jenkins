#!/usr/bin/env groovy
/*
* Name: check_freeze_period
* Author: Vinay Lakshmaiah
* Date: 11-23-2021
*/

import groovy.json.JsonSlurper
import java.time.LocalTime

def call(String repo, String env){
    def parameters_yaml = readYaml file: "pipelines/continous_deployment/conf/services.yaml"
    def freeze_period = parameters_yaml['Services'][repo]['freeze_period']
    def freeze_day = freeze_period['day']
    def freeze_start = freeze_period['start']
    def freeze_end = freeze_period['end']
    if(env == "staging"){
        def new_date = new Date()
        def current_day = new java.text.SimpleDateFormat("EE").format(new_date)
        def current_time = new Date().format( 'HH:mm' )
        if(freeze_day == current_day) {
            LocalTime target = LocalTime.parse( "${current_time}" ) ;
            Boolean targetInZone = ( 
                target.isAfter( LocalTime.parse( "${freeze_start}" ) ) 
                && 
                target.isBefore( LocalTime.parse( "${freeze_end}" ) ) 
            ) ;
            if(targetInZone == true) {
                error "Not Proceeding, I am in freeze period.. Freeze period is every ${current_day} from ${freeze_start} to ${freeze_end} UTC"
            } else {
                echo "I am not freezing.. Proceed"
            }
        
        } else {
            echo "I am not freezing.. Proceed"
        }
        
    }
}