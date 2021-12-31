#!/usr/bin/env groovy
/*
* Name: Artifactory
* Author: Albert Mugisha
* Date: 02-21-2018
*/

def call(params) {
    def account_name = params.account_name ?: ""
    def account_map = [
        "sandbox": "721086286010",
        "trial": "216715641993",
        "staging": "560269805515",
        "production": "745435643501",
        "integration": "472879144981"
    ]
    return account_map[account_name]
}

