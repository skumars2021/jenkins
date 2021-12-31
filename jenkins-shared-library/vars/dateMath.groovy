#!/usr/bin/env groovy
/*
* Name: Start and Stop Node
* Author: Albert Mugisha
* Date: 02-20-2018
*/


@NonCPS
def call(params) {
    Integer minutes = params.minutes ?: 60
    use( groovy.time.TimeCategory ) {
        return_date = (new Date() - minutes.minute)
    }

    return return_date
}