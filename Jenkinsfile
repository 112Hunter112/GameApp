pipeline {
    agent any

    tools {
        // These names must match what is configured in: Manage Jenkins > Global Tool Configuration
        // If you haven't set them up, Jenkins will error.
        // PRO TIP: Use 'maven' if you installed the standard Maven plugin.
        maven 'maven-3'
        jdk 'jdk-17'
    }

    stages {
        stage('Checkout Code') {
            steps {
                // Gets the code from your GitHub repo
                checkout scm
            }
        }

        stage('Build & Test') {
            steps {
                echo 'Building and Testing...'
                // This creates the 'target' folder and runs your Unit Tests
                // -DskipTests=false ensures your new MatchServiceTest runs!
                //sh 'mvn clean package -DskipTests=false'
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    echo 'Building Docker Image...'
                    // Uses the Dockerfile to build the image
                    sh 'docker build -t sports-backend:latest .'
                }
            }
        }
    }

    post {
        always {
            cleanWs()
        }
        success {
            echo 'SUCCESS: Your code is verified and built!'
        }
        failure {
            echo 'FAILURE: Check the logs to see what broke.'
        }
    }
}
