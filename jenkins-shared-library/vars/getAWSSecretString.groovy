#!/usr/bin/env groovy
/*
* Name: Get AWS Secrets Manager SecretString
* Blame: Steven Broderick <steven.broderick@netfoundry.io>
* Date: 09-10-2021
*/

def getSecretString(String secretName, String secretARN) {
    return sh (script: "aws secretsmanager get-secret-value --secret-id ${secretName} --region ${AWS_DEFAULT_REGION} --query SecretString --output text", label: "Reading secret: ${secretARN}", returnStdout: true).trim()
}

def call(String secretARN) {
/*
* AWS ECS Task Definition supports direct key reference
* for multiple key: val, but AWS CLI does not
* Multival secret format:
* arn:aws:secretsmanager:<region>:<account_id>:secret:<secret_name>:<json_key>::
*/
  if (secretARN.contains(':')) {
    String[] arnArray = secretARN.split(':')
    //Sandbox prevents list slicing
    String secretName = arnArray[0,1,2,3,4,5,6].join(':')
    String secretString = getSecretString("${secretName}", "${secretARN}")
  
    if (arnArray.size() > 7) {
      def secretStringJSON = readJSON text: secretString
      return secretStringJSON[arnArray[7]]
    }
    return secretString
  } else {
    return getSecretString("${secretARN}", "${secretARN}")
  }
}
