def call(Map config = [:]) {

    String dockerUser =
        config.dockerUser ?: 'shivakarthikreddyk'

    String backendImage =
        config.backendImage ?: "${dockerUser}/sakila-backend"

    String frontendImage =
        config.frontendImage ?: "${dockerUser}/sakila-frontend"

    String namespace =
        config.namespace ?: 'sakila'

    pipeline {

        agent any

        options {
            timestamps()
            disableConcurrentBuilds()

            buildDiscarder(
                logRotator(
                    numToKeepStr: '10',
                    artifactNumToKeepStr: '5'
                )
            )

            timeout(
                time: 30,
                unit: 'MINUTES'
            )
        }

        environment {
            IMAGE_TAG = "${env.BUILD_NUMBER}"
            BACKEND_IMAGE = "${backendImage}"
            FRONTEND_IMAGE = "${frontendImage}"
            K8S_NAMESPACE = "${namespace}"
        }

        stages {

            stage('Checkout') {
                steps {
                    checkout scm

                    script {
                        env.GIT_COMMIT_SHORT = sh(
                            script: 'git rev-parse --short HEAD',
                            returnStdout: true
                        ).trim()

                        env.IMAGE_TAG =
                            "${env.BUILD_NUMBER}-${env.GIT_COMMIT_SHORT}"
                    }

                    sh 'git log -1 --oneline'
                }
            }

            stage('Validate Repository') {
                steps {
                    script {
                        def requiredPaths = [
                            'backend/Dockerfile',
                            'frontend/Dockerfile',
                            'docker-compose.yml',
                            'k8s'
                        ]

                        requiredPaths.each { path ->
                            if (!fileExists(path)) {
                                error "Required path is missing: ${path}"
                            }
                        }
                    }
                }
            }

            stage('Install and Test') {
                parallel {

                    stage('Backend Test') {
                        steps {
                            script {
                                runNodeTests(
                                    directory: 'backend',
                                    allowMissingTests: true
                                )
                            }
                        }
                    }

                    stage('Frontend Test') {
                        steps {
                            script {
                                runNodeTests(
                                    directory: 'frontend',
                                    allowMissingTests: true
                                )
                            }
                        }
                    }
                }
            }

            stage('Docker Compose Validation') {
                when {
                    expression {
                        return config.get(
                            'runComposeValidation',
                            true
                        )
                    }
                }

                steps {
                    script {
                        validateDockerCompose(
                            timeoutSeconds: 180
                        )
                    }
                }
            }

            stage('Build Docker Images') {
                steps {
                    script {
                        buildDockerImages(
                            backendImage: env.BACKEND_IMAGE,
                            frontendImage: env.FRONTEND_IMAGE,
                            imageTag: env.IMAGE_TAG
                        )
                    }
                }
            }

            stage('Push Docker Images') {
                when {
                    anyOf {
                        branch 'main'
                        branch 'master'
                    }
                }

                steps {
                    script {
                        pushDockerImages(
                            credentialsId: 'dockerhub-credentials',
                            backendImage: env.BACKEND_IMAGE,
                            frontendImage: env.FRONTEND_IMAGE,
                            imageTag: env.IMAGE_TAG
                        )
                    }
                }
            }

            stage('Deploy to Kubernetes') {
                when {
                    anyOf {
                        branch 'main'
                        branch 'master'
                    }
                }

                steps {
                    script {
                        deployToKubernetes(
                            credentialsId:
                                'docker-desktop-kubeconfig',

                            namespace:
                                env.K8S_NAMESPACE,

                            manifestDirectory:
                                'k8s',

                            backendImage:
                                env.BACKEND_IMAGE,

                            frontendImage:
                                env.FRONTEND_IMAGE,

                            imageTag:
                                env.IMAGE_TAG
                        )
                    }
                }
            }

            stage('Verify Deployment') {
                when {
                    anyOf {
                        branch 'main'
                        branch 'master'
                    }
                }

                steps {
                    script {
                        verifyDeployment(
                            credentialsId:
                                'docker-desktop-kubeconfig',

                            namespace:
                                env.K8S_NAMESPACE,

                            rolloutTimeout:
                                180
                        )
                    }
                }
            }
        }

        post {

            success {
                echo """
                Pipeline completed successfully.

                Backend image:
                ${env.BACKEND_IMAGE}:${env.IMAGE_TAG}

                Frontend image:
                ${env.FRONTEND_IMAGE}:${env.IMAGE_TAG}

                Kubernetes namespace:
                ${env.K8S_NAMESPACE}
                """
            }

            failure {
                script {
                    withKubeConfig([
                        credentialsId:
                            'docker-desktop-kubeconfig'
                    ]) {
                        sh """
                            kubectl get all \
                                --namespace ${env.K8S_NAMESPACE} \
                                > kubernetes-status.log 2>&1 || true

                            kubectl get events \
                                --namespace ${env.K8S_NAMESPACE} \
                                --sort-by=.metadata.creationTimestamp \
                                > kubernetes-events.log 2>&1 || true
                        """
                    }
                }

                archiveArtifacts(
                    artifacts: '*.log',
                    allowEmptyArchive: true
                )
            }

            always {
                script {
                    cleanupWorkspace()
                }
            }
        }
    }
}