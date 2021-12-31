def call(command, environment, label){
    def account_id = nfAWSAccounts account_name: "${environment}"
    withAWS(credentials: 'jenkins-central-mgt', region: 'us-east-1', role:'jenkins-central-mgt',  roleAccount: "${account_id}") {
        labelledShell label: "${label}", script: """
            ${command}
        """
    }
}