#!/usr/bin/env groovy
/*
* Name: X ray scanning
* Author: Evan Gertis
* Date: 07-07-2021
*/

import groovy.json.*

def call(params) {

    def rtServer, buildInfo, xrayConfig, xrayResults
    def xrayUser = params.X_RAY_USR
    def xrayPassword = params.X_RAY_PSW
    def buildName = params.BUILD_NAME
    def buildNumber = params.BUILD_NUMBER
    def jobName = params.JOB_NAME

    rtServer = Artifactory.newServer url: 'jenkins-x-ray-server'
    buildInfo = Artifactory.newBuildInfo()


    rtBuildInfo (
        buildName: buildName,
        buildNumber: buildNumber,
        maxBuilds: 1,
        maxDays: 2,
        dooNotDiscardBuilds: ["3"],
        deleteBuildArtifacts: true
    )

    rtPublishBuildInfo (
        serverId: 'x-ray-server',
        buildName: jobName,
        buildNumber: buildNumber 
    )

    xrayScan (
        serverId: 'x-ray-server',
        buildName: jobName,
        buildNumber: buildNumber,
        failBuild: false
    )


}