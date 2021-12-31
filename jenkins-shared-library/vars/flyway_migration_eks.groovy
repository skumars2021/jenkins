#!/usr/bin/env groovy
/*
* Name: flyway_migration_eks
* Author: Vinay Lakshmaiah
* Date: 11-23-2021
*/

import groovy.json.JsonSlurper
import org.apache.commons.lang3.StringUtils;
import hudson.model.*

def call(repo, environment, namespace, CENTOS_CONTAINER){
    def account_id = nfAWSAccounts account_name: "${environment}"
    def db_global_user = "root"
    withAWS(credentials: 'jenkins-central-mgt', region: 'us-east-1', role:'jenkins-central-mgt',  roleAccount: "${account_id}") {
        def parameters_yaml = readYaml file: "pipelines/continous_deployment/conf/services.yaml"
        def flyway_ignore_text_array = parameters_yaml["flyway_block_migration_text"]
        def yamlServices = parameters_yaml["Services"]
        if ( repo == "network") {
            dbhost = "nfapidb"
        } else if(yamlServices[repo]["database"] != null){
            dbhost = yamlServices[repo]["database"] 
        } else {
            echo "Service doesnot have a database."
            return
        }

        def migration_path = parameters_yaml["Databases"]["${dbhost}"]['migration_path']
        def db_names = parameters_yaml["Databases"]["${dbhost}"]["dbname"]
        def k8s_migration_path = parameters_yaml["Databases"]["${dbhost}"]["k8s_migration_path"]
        def centContainer = CENTOS_CONTAINER
        dir("${repo}"){
            for ( String path: migration_path.split(",")) {
                localPath = path.split(":")[1]
                labelledShell label: "Copy Migration Files ${localPath} to Container", script: """
                    kubectl cp ${localPath} ${centContainer}:/ --namespace=${namespace}
                """
            }

            // @FIXME - make this a parameter and actually reuse this function. We shouldn't stop promotions if they've already passed PR checks
            ignore_flyway_validation = true
            
            if( ignore_flyway_validation == false) {
                commandValidation = "/flyway-6.4.0/flyway validate -user=root -password=DockerRoot9876 -url=jdbc:mysql://${dbhost}:3306/${db_names[0]} -locations=${k8s_migration_path} 2>&1|| true"
                def validateCommand = sh( script: "kubectl exec --namespace ${namespace} ${centContainer} -- bash -c \"${commandValidation}\"", label: "Flyway Validate", returnStdout: true)
                
                for( String line: validateCommand.split("\n")) {
                    if(line.contains("Detected resolved migration not applied to database")){
                        def databaseVersion = line.split(":")[1].trim()
                        for(String flyway_ignore_str: flyway_ignore_text_array){
                            commandValidation = "grep '${flyway_ignore_str}' ./*migration*/*${databaseVersion}* | wc -l"
                            def errorCount = sh( script: "kubectl exec --namespace ${namespace} ${centContainer} -- bash -c \"${commandValidation}\"", label: "Get Migration Error Count", returnStdout:true).trim()
                            if( "${errorCount.replaceAll(' ','')}" != "0"){
                                error "The SQL Migration file ${databaseVersion} contains restricted keyword ${flyway_ignore_str}. This indicates you are trying to modify an existing column which is not allowed."
                            }
                        }
                    }
                }
            }
            
            command = "/flyway-6.4.0/flyway migrate -user=${db_global_user} -password=DockerRoot9876 -url=jdbc:mysql://${dbhost}:3306/${db_names[0]} -locations=${k8s_migration_path}"
            labelledShell label: "Flyway Migration", script: """
                kubectl exec --namespace ${namespace} ${centContainer} -- bash -c \"${command}\"
            """
        }
    }
}