import org.jenkinsci.plugins.pipeline.modeldefinition.Utils

windows = false
linux = false
mac = false
sandbox = false
staging = false
production = false
error_msg = null

pipeline {
    agent any
    parameters {
        choice(name:'environment', choices: "sandbox\nstaging\nproduction", description: 'Environment to Run tests')
        booleanParam(defaultValue: false, name: 'liverun', description: 'Select type of run:')
        text(defaultValue: 'this is a multi-line string parameter example here', name: 'text: ', description: 'Text to check the value')
        string(defaultValue: 'Enter the script to run here', name: 'scriptline : ', trim: true, description: 'Text to check the value')
    }
    stages{
        stage('Build'){
            steps{
                script{
                    try {
                        echo 'Deployed the code Complited....'
                    } catch (e) {
                        error "${e}"
                    }
                    
                }
            }
        }
        stage('Unit Test'){
            steps{
                script{
                    try {
                        echo 'Unit Testing Complited....'
                    } catch (e) {
                        error "${e}"
                    }
                    
                }
            }
        }
        stage('Smoke Test'){
            steps{
                script{
                    try {
                        echo 'Smoke Testing Complited....'
                    } catch (e) {
                        error "${e}"
                    }
                    
                }
            }
        }
        stage('Regression Tests') {
            steps {
                parallel(
                        "Regression on Chrome": {
                            script {
                               echo "Regression PASSED Chrome"
                               if(params.environment == "sandbox"){
                                   sandbox = true  
                               }
                               
                            }
                        },
                        "Regression on FF": {
                            script {
                               echo "Regression PASSED FF"
                               if(params.environment == "staging"){
                                   staging = true  
                               }
                               
                            }
                        },
                        "Regression on IE": {
                            script {
                               echo "Regression PASSED IE"
                               if(params.environment == "production"){
                                   production = true  
                               }
                               
                            }
                            
                        }
                )
            }
        }
        stage('Deploy to ENV') {
            steps {
                parallel(
                        "Deploy to Sandbox": {
                            script {
                                if(sandbox){
                                echo "Deploy to Sandbox Passed"
                                echo "{$sandbox}"
                                    
                                }
                                
                            }
                            
                        },
                        "Deploy to Staging": {
                            script {
                                if(staging){
                                echo "Deploy to Staging Passed"
                                echo "{$staging}"
                                }
                            }
                            
                        },
                        "Deploy to Production": {
                            script {
                                if(production){
                                echo "Deploy to Production Passed"
                                echo "{$production}"
                                }
                                
                            }
                            
                        }
                )
            }
        }
        stage("End Build"){
            steps{
                script{
                    try {
                        echo 'End build Performing steps of  Complited....'
                        //sh "printenv"
                        echo "The build number is ${env.BUILD_NUMBER}"
                        echo "NAME = ${env.NAME}"
                        echo "running on = ${env.NODE_LABELS}"
                        commitId = sh(returnStdout: true, script: 'git rev-parse HEAD')
                        generate_report_date()
                        echo "${commitId}"
                    } catch (e) {
                        error "${e}"
                    }
                    
                }
            }
        }

    
    }
}


def generate_report_date(){
    def date = new Date().format("yyMMdd.HHmm", TimeZone.getTimeZone('UTC'))
    echo "date with method : ${date}"
}
