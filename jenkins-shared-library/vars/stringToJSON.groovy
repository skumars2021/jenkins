#!/usr/bin/env groovy
/*
* Name: Start and Stop Node
* Author: Albert Mugisha
* Date: 02-20-2018
*/

def call(params) {
    def str = params.str
    def resultJSON = new groovy.json.JsonSlurper().parseText(str)
    return resultJSON
}