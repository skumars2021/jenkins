pipeline {
    agent any
    
    parameters {
        choice(choices: ['ONE', 'TWO'], name: 'PARAMETER_01', description: 'Environment to Run tests'),
        booleanParam(defaultValue: true, description: '', name: 'BOOLEAN'),
        text(defaultValue: '''this is a multi-line string parameter example''', name: 'MULTI-LINE-STRING'),
        string(defaultValue: 'scriptcrunch', name: 'STRING-PARAMETER', trim: true)
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
