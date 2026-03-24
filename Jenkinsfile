pipeline {
    agent any

    options {
        timestamps()
        ansiColor('xterm')
    }

    environment {
        REGISTRY = 'docker.io/your-dockerhub-user'
        BACKEND_IMAGE = "${REGISTRY}/devops-backend"
        FRONTEND_IMAGE = "${REGISTRY}/devops-frontend"
        IMAGE_TAG = "${BUILD_NUMBER}"
        KUBE_NAMESPACE = 'devops-app'
        SLACK_CHANNEL = '#bombergame'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
            post {
                always {
                    script {
                        notifySlack('Checkout', currentBuild.currentResult ?: 'SUCCESS')
                    }
                }
            }
        }

        stage('Backend Unit Tests') {
            steps {
                dir('backend') {
                    sh 'mvn -B test'
                }
            }
            post {
                always {
                    script {
                        notifySlack('Backend Unit Tests', currentBuild.currentResult ?: 'SUCCESS')
                    }
                }
            }
        }

        stage('Build Backend Artifact') {
            steps {
                dir('backend') {
                    sh 'mvn -B -DskipTests clean package'
                }
            }
            post {
                always {
                    script {
                        notifySlack('Build Backend Artifact', currentBuild.currentResult ?: 'SUCCESS')
                    }
                }
            }
        }

        stage('Build Docker Images') {
            steps {
                script {
                    sh "docker build -t ${BACKEND_IMAGE}:${IMAGE_TAG} backend"
                    sh "docker build -t ${FRONTEND_IMAGE}:${IMAGE_TAG} frontend"
                }
            }
            post {
                always {
                    script {
                        notifySlack('Build Docker Images', currentBuild.currentResult ?: 'SUCCESS')
                    }
                }
            }
        }

        stage('Push Docker Images') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub-credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                    sh 'echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin'
                    sh "docker push ${BACKEND_IMAGE}:${IMAGE_TAG}"
                    sh "docker push ${FRONTEND_IMAGE}:${IMAGE_TAG}"
                }
            }
            post {
                always {
                    script {
                        notifySlack('Push Docker Images', currentBuild.currentResult ?: 'SUCCESS')
                    }
                }
            }
        }

        stage('Deploy To Kubernetes') {
            steps {
                withCredentials([file(credentialsId: 'kubeconfig', variable: 'KUBECONFIG')]) {
                    sh "kubectl apply -f k8s/namespace.yaml"
                    sh "kubectl apply -f k8s/mysql.yaml"
                    sh "kubectl apply -f k8s/backend.yaml"
                    sh "kubectl apply -f k8s/frontend.yaml"
                    sh "kubectl -n ${KUBE_NAMESPACE} set image deployment/backend backend=${BACKEND_IMAGE}:${IMAGE_TAG}"
                    sh "kubectl -n ${KUBE_NAMESPACE} set image deployment/frontend frontend=${FRONTEND_IMAGE}:${IMAGE_TAG}"
                }
            }
            post {
                always {
                    script {
                        notifySlack('Deploy To Kubernetes', currentBuild.currentResult ?: 'SUCCESS')
                    }
                }
            }
        }

        stage('Verify Rollout') {
            steps {
                withCredentials([file(credentialsId: 'kubeconfig', variable: 'KUBECONFIG')]) {
                    sh "kubectl -n ${KUBE_NAMESPACE} rollout status deployment/mysql --timeout=180s"
                    sh "kubectl -n ${KUBE_NAMESPACE} rollout status deployment/backend --timeout=180s"
                    sh "kubectl -n ${KUBE_NAMESPACE} rollout status deployment/frontend --timeout=180s"
                }
            }
            post {
                always {
                    script {
                        notifySlack('Verify Rollout', currentBuild.currentResult ?: 'SUCCESS')
                    }
                }
            }
        }
    }

    post {
        success {
            script {
                notifySlack('Pipeline', 'SUCCESS')
            }
        }
        failure {
            script {
                notifySlack('Pipeline', 'FAILURE')
            }
        }
        unstable {
            script {
                notifySlack('Pipeline', 'UNSTABLE')
            }
        }
    }
}

def notifySlack(String stageName, String status) {
    String color = 'danger'
    if (status == 'SUCCESS') {
        color = 'good'
    } else if (status == 'UNSTABLE') {
        color = 'warning'
    }

    slackSend(
        channel: env.SLACK_CHANNEL,
        color: color,
        message: "${env.JOB_NAME} #${env.BUILD_NUMBER} | ${stageName} | ${status} | ${env.BUILD_URL}"
    )
}
