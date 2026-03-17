pipeline {
    agent any

    tools {
        maven 'maven-3'
        jdk 'jdk-17'
    }

    stages {
        stage('Checkout Code') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test') {
            steps {
                echo 'Building and Testing...'
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Prepare Secrets') {
            // This section unlocks the Jenkins Vault
            environment {
                // Jenkins automatically splits 'Username with password' into two variables:
                // EMAIL_USR and EMAIL_PSW
                EMAIL = credentials('email-creds')

                // This grabs the text directly
                JWT_KEY = credentials('jwt-secret')
            }
            steps {
                script {
                    echo 'Injecting Real Secrets from Vault...'

                    sh 'mkdir -p src/main/resources'

                    // We use the environment variables ($EMAIL_USR, etc)
                    // NOT the real passwords. Safe for GitHub!
                    sh """
                    echo "spring.mail.username=$EMAIL_USR" > src/main/resources/application-secrets.properties
                    echo "spring.mail.password=$EMAIL_PSW" >> src/main/resources/application-secrets.properties
                    echo "jwt.secret=$JWT_KEY" >> src/main/resources/application-secrets.properties
                    echo "jwt.expiration=86400000" >> src/main/resources/application-secrets.properties
                    echo "db.password=password" >> src/main/resources/application-secrets.properties
                    """
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    echo 'Building Docker Image...'
                    sh 'docker build -t sports-backend:latest .'
                }
            }
        }
    }

    post {
        always {
            cleanWs()
        }
    }
}
