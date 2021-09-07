pipeline {
    agent any
    
    parameters {
        choice(choices: ['ONE', 'TWO'], name: 'PARAMETER_01', description: 'Select choice'),
        booleanParam(defaultValue: true, description: '', name: 'PARAMETER_02 booleanParam'),
        text(defaultValue: '''this is a multi-line string parameter example''', name: 'PARAMETER_03 MULTI-LINE-STRING'),
        string(defaultValue: 'scriptcrunch', name: 'PARAMETER_04 STRING-PARAMETER', trim: true)
    }

    stages {
        stage('Hello') {
            steps {
                echo 'Hello World'
            }
            
        }
        stage('Pull') {
            steps {
                echo 'Code Pull'
            }
        }
        stage('Depoly') {
            steps {
                echo 'Hello Depoly'
            }
        }
        stage('Test') {
            steps {
                echo 'Hello Test'
            }
        }
        stage('production') {
            steps {
                echo 'Hello production'
            }
        }
        
    }
}
