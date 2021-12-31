#!/usr/bin/env groovy
/*
* Name: build_databases
* Author: Vinay Lakshmaiah
* Date: 11-25-2021
*/

import groovy.json.JsonSlurper
import org.apache.commons.lang3.StringUtils;
import hudson.model.*

def call(environment, namespace, cluster_name, database_changeset, CENTOS_CONTAINER, HELM_TIMEOUT, helm_verbose_level){
    def account_id = nfAWSAccounts account_name: "${environment}"
    def buildDatabases = [:]
    def parameters_yaml = readYaml file: "pipelines/continous_deployment/conf/services.yaml"
    def parameters = parameters_yaml["Databases"]
    echo "${parameters}"
    
    for(String db: database_changeset) {
        def database = "${db}"
        buildDatabases["${database}"] = { ->
            def db_set = parameters["${database}"]
            def db_import = db_set["import"]
            if( db_import == "true") {
                dir("nf-mop-kubernetes"){
                    def db_selected = "${database}"
                    def db_user = db_set["username"]
                    def dbname = db_set["dbname"]
                    def db_secret = db_set["aws_secret"]
                    def db_identifier = db_set["db_identifier"]
                    def db_version = db_set["version"]
                    def helm_release = "${db_selected}-${namespace}-mysql"
                    
                    if(helm_release.size() > 50) {
                        helm_release = "${db_selected}-${namespace}-mysql".substring(0,50)+"a"
                    }

                    try {
                        run_eks_command("helm delete ${helm_release} --namespace ${namespace} || true", environment, "Predeploy cleanup - remove helm chart")
                    } catch (e) {
                        // echo "${helm_release} release doesnt exist"
                    }

                    withAWS(credentials: 'jenkins-central-mgt', region: 'us-east-1', role:'jenkins-central-mgt',  roleAccount: "${account_id}") {
                        def db_password = getAWSSecretString "${db_secret}"
                        def db_secret_query = db_set["aws_secret_query"]
                        if(db_secret_query != null) {
                            db_password_json = readJSON text: db_password
                            db_password = db_password_json[db_secret_query]
                        }
                        def db_endpoint = sh(script: "aws rds describe-db-instances --db-instance-identifier ${db_identifier} --region us-east-1 --query 'DBInstances[0].Endpoint.Address' --output text",label:"Retrieve DB Endpoint",returnStdout: true).trim()
                        labelledShell label: "Create Database Container. Name -> ${helm_release}", script: """
                            kubectl config use-context ${cluster_name}; helm install ${helm_release} mysql_chart/ --set db_version=${db_version} --set name=${db_selected} --set namespace=${namespace} --namespace ${namespace} --timeout=${HELM_TIMEOUT} --wait -v=${helm_verbose_level}
                        """

                        for(String db_entry: dbname) {
                            def DUMP_DATABASE = "mysqldump -h${db_endpoint} -u${db_user} ${db_entry} --single-transaction --quick --lock-tables=false > ${environment}-${namespace}-${db_entry}.sql;"
                            withEnv(["MYSQL_PWD=${db_password}"]){
                                if( db_entry == "nfapidb") {
                                    labelledShell label: "Truncate endpoints_aud table", script: """
                                        mysql -h${db_endpoint} -u ${db_user} -D ${db_entry} -e  'TRUNCATE TABLE endpoints_aud'
                                    """
                                }
                                def NON_AUD_TABLES = "mysql -h ${db_endpoint} -u ${db_user} -Bse \"SHOW TABLES FROM ${db_entry} WHERE Tables_in_${db_entry} NOT LIKE '%aud' AND  Tables_in_${db_entry} != 'revinfo'\""
                                def AUD_TABLES = "mysql -h ${db_endpoint} -u ${db_user} -Bse \"SHOW TABLES FROM ${db_entry} WHERE Tables_in_${db_entry} LIKE '%aud' OR  Tables_in_${db_entry} = 'revinfo'\""
                                labelledShell label: "Dump mysql tables", script: """
                                    mysqldump -h${db_endpoint} -u${db_user} ${db_entry} \$(${NON_AUD_TABLES}) --single-transaction --quick --lock-tables=false > sandbox-${namespace}-${db_entry}-1.sql;
                                    mysqldump -h${db_endpoint} -u${db_user} ${db_entry} \$(${AUD_TABLES}) --no-data --single-transaction --quick --lock-tables=false > sandbox-${namespace}-${db_entry}-2.sql;
                                    sed -i 's/COLLATE=utf8mb4_0900_ai_ci//g' sandbox-${namespace}-${db_entry}-1.sql
                                    sed -i 's/COLLATE=utf8mb4_0900_ai_ci//g' sandbox-${namespace}-${db_entry}-2.sql
                                """
                            }
                            // Change Collation
                            def datacontainer=sh (script: "kubectl get pods --namespace=${namespace} | grep ${db_selected} | cut -d' ' -f1", label:"Retrieve running container",returnStdout: true).trim()
                            try {
                                labelledShell label: "Copy mysql dumps to container", script: """
                                    kubectl cp sandbox-${namespace}-${db_entry}-1.sql ${datacontainer}:/ --namespace=${namespace}
                                    kubectl config use-context ${cluster_name}; kubectl cp sandbox-${namespace}-${db_entry}-2.sql ${datacontainer}:/ --namespace=${namespace}
                                """
                            } catch (exm) {
                                sleep 10
                                labelledShell label: "Copy mysql dumps to container", script: """
                                    kubectl cp sandbox-${namespace}-${db_entry}-1.sql ${datacontainer}:/ --namespace=${namespace}
                                    kubectl config use-context ${cluster_name}; kubectl cp sandbox-${namespace}-${db_entry}-2.sql ${datacontainer}:/ --namespace=${namespace}
                                """
                            }
                            // Create Database
                            def sqlCreateCommand = "mysql -u root -p'DockerRoot9876' -e 'CREATE DATABASE ${db_entry}'"
                            def createCommand = "kubectl config use-context ${cluster_name};kubectl exec --namespace ${namespace} ${datacontainer} -- bash -c \"${sqlCreateCommand}\""
                            try {
                                labelledShell label: "Create database ${db_entry} in EKS Container", script: """
                                    ${createCommand}
                                """
                            } catch (exm) {
                                labelledShell label: "Create database ${db_entry} in EKS Container", script: """
                                    sleep 10
                                    ${createCommand}
                                """   
                            }

                            // Import Database
                            def sqlImportCommand = "mysql -u root -p'DockerRoot9876' -D ${db_entry} < sandbox-${namespace}-${db_entry}-1.sql"
                            def importCommand = "kubectl config use-context ${cluster_name}; kubectl exec --namespace ${namespace} ${datacontainer} -- bash -c \"${sqlImportCommand}\""
                            def sqlImportCommand2 = "mysql -u root -p'DockerRoot9876' -D ${db_entry} < sandbox-${namespace}-${db_entry}-2.sql"
                            def importCommand2 = "kubectl config use-context ${cluster_name}; kubectl exec --namespace ${namespace} ${datacontainer} -- bash -c \"${sqlImportCommand2}\""
                            
                            try {
                                labelledShell label: "Import non AUDIT tables into ${db_entry}", script: """
                                    ${importCommand}
                                """
                            } catch (exm) {
                                labelledShell label: "Import non AUDIT tables into ${db_entry}", script: """
                                    sleep 10
                                    ${importCommand}
                                """
                            }
                            if( db_entry == "nfapidb") {
                                try {
                                    labelledShell label: "Import AUDIT tables into ${db_entry}", script: """
                                        ${importCommand2}
                                    """
                                } catch (exm) {
                                    labelledShell label: "Import non AUDIT tables into ${db_entry}", script: """
                                        sleep 10
                                        ${importCommand2}
                                    """ 
                                }
                            }

                            try {
                                def command = "kubectl exec --namespace ${namespace} ${CENTOS_CONTAINER} -- bash -c \"nc -vw1 ${db_selected} 3306 </dev/null\""
                                check_service_availability("${db_selected}",300, command)
                            } catch(exm) {
                                echo "${exm}"
                            }
                            labelledShell label: "Cleanup MySQL Dumps", script: """
                                rm  -f sandbox-${namespace}-${db_entry}-1.sql
                                rm  -f sandbox-${namespace}-${db_entry}-2.sql
                            """
                        }
                    }
                }
            } else {
                dir("nf-mop-kubernetes"){
                    def db_selected1 = "${database}"
                    def helm_release1 = "${db_selected1}-${namespace}-postgres"
                    if(helm_release1.size() > 50) {
                        helm_release1 = "${db_selected1}-${namespace}-postgres".substring(0,50)+"a"
                    }

                    try {
                        run_eks_command("helm delete ${helm_release1} --namespace ${namespace} || true", environment, "Delete previous running Containers")
                    } catch (e) {
                        echo "${helm_release1} release doesnt exist"
                    }

                    withAWS(credentials: 'jenkins-central-mgt', region: 'us-east-1', role:'jenkins-central-mgt',  roleAccount: "${account_id}") {
                        sh "kubectl config use-context ${cluster_name}; helm install ${helm_release1} postgres_chart/ --set db_version=11.8 --set name=${db_selected1} --set namespace=${namespace} --namespace ${namespace} --timeout=${HELM_TIMEOUT} --wait -v=${helm_verbose_level}"
                    }
                }
            }
        }
    }
    parallel buildDatabases
}