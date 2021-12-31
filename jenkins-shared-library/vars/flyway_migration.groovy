#!/usr/bin/env groovy
/*
* Name: flyway_migration
* Author: Vinay Lakshmaiah
* Date: 11-19-2021
*/

import groovy.json.JsonSlurper

def call(String repo, String db, String dep_env) {
    def parameters_yaml = readYaml file: "pipelines/continous_deployment/conf/services.yaml"
    def database = parameters_yaml["Databases"][db]
    def db_identifier = database['db_identifier']
    def dbname = database['dbname'][0]
    def dbuser = database['username']
    def secret = database['aws_secret']
    
    def migration_path = database['migration_path']
    def account_id = nfAWSAccounts account_name: "${dep_env}"
    withAWS(credentials: 'jenkins-central-mgt', region: 'us-east-1', role:'jenkins-central-mgt',  roleAccount: "${account_id}") {
        db_secret = getAWSSecretString "${secret}"
        def db_secret_query = database["aws_secret_query"]
        if(db_secret_query != null) {
            db_password = readJSON text: db_secret
            db_secret = db_password_json[db_secret_query]
        }
        def dbhost = sh(script: "aws rds describe-db-instances --db-instance-identifier ${db_identifier} --region us-east-1 --query 'DBInstances[0].Endpoint.Address' --output text",returnStdout: true).trim()
        dir("${repo}") {
            withEnv(["FLYWAY_USER=${dbuser}", "FLYWAY_PASSWORD=${db_secret}"]) {
                 //if(repo == "network") {
                 //  sh "mysql -h${dbhost} -D ${dbname} -e  'TRUNCATE TABLE endpoints_aud'"
                 //}
                sh "flyway migrate -url=jdbc:mysql://${dbhost}:3306/${dbname} -locations=${migration_path}"
            }
        }
    }
}