#!/usr/bin/env groovy
/*
* Name: Prompt for input of temporary AWS credentials vars
* Blame: Ken
* Date: 04-17-2020
*
* AWS CLI example: 
*    aws sts assume-role --role-arn arn:aws:iam::721086286010:role/ops-mgmt-admin --role-session-name assumeTest123 | jq -r '.Credentials|.AccessKeyId+":"+.SecretAccessKey'
*
* Usage:- def temp_creds = getAwsTempCreds environment: "sandbox" 
*/

def call(params) {
    def dep_environment = params.environment ?: "sandbox"
    def target_role = params.role ?: "bastion-admin-role"
    def account_id = nfAWSAccounts account_name: dep_environment
    def new_timestamp = new Date().format( 'yyyyMMddHHmm' )
    def command = 'aws sts assume-role --role-arn arn:aws:iam::'+account_id+':role/'+target_role+' --role-session-name jenkins-'+new_timestamp+'|jq -r ".Credentials|.SessionToken+\\\":\\\"+.AccessKeyId+\\\":\\\"+.SecretAccessKey"'
    sh(script: "echo '${command}'", label: "Expand to view STS command to retrieve temporary token")
    def message = "Please enter temporary AWS session token for an authorized role in the ${dep_environment} account.\nTo retrieve this key, issue STS command ( printed above ) from CLI while connected to nf-ops-mgt account\n"
    temp_creds = input   message: "${message}", 
            ok: 'Submit',
            parameters: [ password(name: 'tempCred') ],
            submitterParameter: 'approvedUser'
    return temp_creds
}