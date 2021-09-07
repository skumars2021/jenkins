windows = false
linux = false
mac = false
sandbox = false
staging = false
error_msg = null

pipeline {
    agent any
    parameters {
        choice(defaultValue: 'sandbox', name:'environment', choices: "sandbox\nstaging\nproduction", description: 'Environment to Run tests')
        booleanParam(defaultValue: false, name: 'liverun', description: 'Select type of run:')
        text(defaultValue: 'this is a multi-line string parameter example here', name: 'text: ', description: 'Text to check the value')
        string(defaultValue: 'Enter the script to run here', name: 'Script : ', trim: true, description: 'Text to check the value')
    }

    if(params.liverun == true){
        stages {
            stage('Build') {
                script {
                    try {
                        echo 'Deployed the code Complited....'
                    } catch(e){
                        error_msg = "DeployCode Failed"
                        error "${error_msg}"
                    } 
                } 
            }
            stage('Unit Test') {
                script {
                    try {
                        echo 'Unit Testing Complited....'
                    } catch(e){
                        error_msg = "Unit Test Failed"
                        error "${error_msg}"
                    } 
                } 
            }
            stage('Smoke Test') {
                script {
                    try {
                        echo 'Smoke Testing Complited...'
                    } catch(e){
                        error_msg = "Smoke Test Failed"
                        error "${error_msg}"
                    } 
                } 
            }

            if(params.liverun == 'sandbox'){
                parallel {
                    stage('Regression Test with Chrome with linux') {
                        script {
                            echo 'Regression Test On Chrome with linux Complited...'
                            linux = true
                        } 
                    } 
                    stage('Regression Test with Firefox with windows') {
                        script {
                            echo 'Regression Test On Firefox with windows Complited...'
                            windows = true
                        } 
                    }
                    stage('Regression Test with Safar on MAC') {
                        script {
                            echo 'Regression Smoke Testing Complited...'
                            mac = true
                        } 
                    }
                }
                stage('Deploy to Sandbox') {
                    if(mac == true && windows == true && linux == true){
                        script {
                            echo 'Deployed to Sandbox Complited...'
                            sandbox = true
                        }
                    }else{
                      error_msg = "Deployed to Sandbox Failed"
                      error "${error_msg}"
                    }

                }
                
            }else{
                error_msg = "Deployed to Sandbox Failed"
                error "${error_msg}"
            }

            }
            
        }

    }
}
