pipeline {
    agent any
    environment {
        REGISTRY = 'my-docker-registry'   // Registro Docker
        REPO = 'my-app-repo'              // Repositorio Docker
        IMAGE_TAG = "${env.BUILD_ID}"     // Etiqueta de imagen única
        KUBE_CONFIG = credentials('kube-config') // Credenciales para Kubernetes
    }
    stages {
        stage('Checkout') {
            steps {
                // Obtener el código desde GitHub
                git branch: 'main', url: 'https://github.com/my-org/my-app.git'
            }
        }

        stage('Build') {
            steps {
                // Compilar el código (ejemplo con Maven)
                sh 'mvn clean install'
            }
        }

        stage('Static Code Analysis') {
            steps {
                // Análisis de código estático con SonarQube
                withSonarQubeEnv('SonarQube') {
                    sh 'mvn sonar:sonar'
                }
            }
        }

        stage('Unit Tests') {
            steps {
                // Ejecutar pruebas unitarias
                sh 'mvn test'
            }
        }

        stage('Build Docker Image') {
            steps {
                // Construir la imagen Docker y empujarla al registro
                script {
                    dockerImage = docker.build("${env.REGISTRY}/${env.REPO}:${env.IMAGE_TAG}")
                    docker.withRegistry('https://my-docker-registry.com', 'docker-credentials') {
                        dockerImage.push()
                    }
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                // Desplegar en Kubernetes usando kubectl
                withCredentials([file(credentialsId: 'kube-config', variable: 'KUBECONFIG')]) {
                    sh '''
                    kubectl apply -f kubernetes/deployment.yml
                    kubectl apply -f kubernetes/service.yml
                    '''
                }
            }
        }
    }
    post {
        success {
            // Notificación en caso de éxito
            mail to: 'team@example.com',
                 subject: "Deployment Successful: ${env.JOB_NAME} - Build #${env.BUILD_NUMBER}",
                 body: "The application has been deployed successfully. Check the logs for details."
        }
        failure {
            // Notificación en caso de fallo
            mail to: 'team@example.com',
                 subject: "Deployment Failed: ${env.JOB_NAME} - Build #${env.BUILD_NUMBER}",
                 body: "The deployment has failed. Check the Jenkins logs for more details."
        }
    }
}
