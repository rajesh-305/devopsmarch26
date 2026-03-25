pipeline {
    agent any

    options {
        timestamps()
    }

    environment {
        REGISTRY = 'docker.io/dataquaint'
        BACKEND_IMAGE = "${REGISTRY}/devops-backend"
        FRONTEND_IMAGE = "${REGISTRY}/devops-frontend"
        IMAGE_TAG = "${BUILD_NUMBER}"
        KUBE_NAMESPACE = 'devops-app'
        SLACK_CHANNEL = '#bombergame'
        SONARQUBE_SERVER = 'sonarqube-server'
        SONAR_PROJECT_KEY = 'three-tier-app'
        TRIVY_SEVERITY = 'HIGH,CRITICAL'
        TRIVY_DISABLE_VEX_NOTICE = 'true'
        TRIVY_CACHE_DIR = '.trivycache'
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

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv("${SONARQUBE_SERVER}") {
                    dir('backend') {
                        sh "mvn -B sonar:sonar -DskipTests -Dsonar.projectKey=${SONAR_PROJECT_KEY} -Dsonar.projectName=three-tier-app"
                    }
                }
            }
            post {
                always {
                    script {
                        notifySlack('SonarQube Analysis', currentBuild.currentResult ?: 'SUCCESS')
                    }
                }
            }
        }

        stage('SonarQube Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
            post {
                always {
                    script {
                        notifySlack('SonarQube Quality Gate', currentBuild.currentResult ?: 'SUCCESS')
                    }
                }
            }
        }

        stage('Trivy Filesystem Scan') {
            steps {
                sh '''#!/bin/bash
set -e
if ! command -v trivy >/dev/null 2>&1; then
  echo "Trivy is not installed on this Jenkins agent."
  exit 1
fi
trivy fs --scanners vuln --no-progress --severity ${TRIVY_SEVERITY} --exit-code 1 .
'''
            }
            post {
                always {
                    script {
                        notifySlack('Trivy Filesystem Scan', currentBuild.currentResult ?: 'SUCCESS')
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

        stage('Docker Daemon Check') {
            steps {
                sh '''#!/bin/bash
set -e
if ! command -v docker >/dev/null 2>&1; then
  echo "Docker CLI is not installed on this Jenkins agent."
  exit 1
fi

if ! docker version >/dev/null 2>&1; then
  echo "Jenkins cannot access Docker daemon."
  echo "Fix by adding jenkins user to docker group and restarting Jenkins agent."
  echo "Linux host example: sudo usermod -aG docker jenkins && sudo systemctl restart jenkins"
  exit 1
fi

docker version
'''
            }
            post {
                always {
                    script {
                        notifySlack('Docker Daemon Check', currentBuild.currentResult ?: 'SUCCESS')
                    }
                }
            }
        }

        stage('Build Docker Images') {
            steps {
                script {
                    sh "docker build --pull -t ${BACKEND_IMAGE}:${IMAGE_TAG} backend"
                    sh "docker build --pull -t ${FRONTEND_IMAGE}:${IMAGE_TAG} frontend"
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

        stage('Trivy Image Scan') {
            steps {
                sh "trivy image --scanners vuln --no-progress --ignore-unfixed --severity ${TRIVY_SEVERITY} --exit-code 1 ${BACKEND_IMAGE}:${IMAGE_TAG}"
                sh "trivy image --scanners vuln --no-progress --ignore-unfixed --severity ${TRIVY_SEVERITY} --exit-code 1 ${FRONTEND_IMAGE}:${IMAGE_TAG}"
            }
            post {
                always {
                    script {
                        notifySlack('Trivy Image Scan', currentBuild.currentResult ?: 'SUCCESS')
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
                    sh 'docker logout || true'
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

        stage('Docker Compose Integration Test') {
            steps {
                sh '''#!/bin/bash
set -e
PROJECT_NAME="ci-${BUILD_NUMBER}"
BACKEND_PORT=$((20000 + BUILD_NUMBER % 10000))
FRONTEND_PORT=$((30000 + BUILD_NUMBER % 10000))

export COMPOSE_PROJECT_NAME="${PROJECT_NAME}"
export BACKEND_HOST_PORT="${BACKEND_PORT}"
export FRONTEND_HOST_PORT="${FRONTEND_PORT}"

cleanup() {
  docker compose down -v --remove-orphans || true
}

trap cleanup EXIT

docker compose up -d --build

for i in {1..30}; do
  if curl -fsS "http://localhost:${BACKEND_HOST_PORT}/api/notes" >/dev/null; then
    break
  fi
  sleep 5
done

curl -fsS "http://localhost:${BACKEND_HOST_PORT}/api/notes" >/dev/null
curl -fsS "http://localhost:${FRONTEND_HOST_PORT}/" >/dev/null
'''
            }
            post {
                always {
                    script {
                        notifySlack('Docker Compose Integration Test', currentBuild.currentResult ?: 'SUCCESS')
                    }
                }
            }
        }

        stage('Deploy To Kubernetes') {
            steps
            {
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
