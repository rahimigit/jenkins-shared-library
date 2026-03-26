/**
 * techmekBuild — Standard Kaniko build pipeline step for TechMek apps.
 *
 * Usage in Jenkinsfile:
 *   @Library('techmek-shared-library') _
 *   techmekBuild(
 *       appDir: 'gitops/apps/myapp',
 *       imageName: 'admin/myapp',
 *       appVersion: '1.0.0'
 *   )
 *
 * Parameters:
 *   appDir      — Path to Dockerfile context (required)
 *   imageName   — Registry image path e.g. 'admin/myapp' (required)
 *   appVersion  — Semantic version (required)
 *   registry    — External registry domain (default: gitea.techmek.io)
 *   registryInt — Internal registry for push (default: gitea.gitea.svc.cluster.local)
 *   dockerfile  — Dockerfile path relative to appDir (default: Dockerfile)
 *   cache       — Enable Kaniko cache layer (default: false)
 *   skipDeploy  — Skip kubectl rollout restart (default: false)
 *   namespace   — Kubernetes namespace for deployment (default: derived from imageName)
 *   deployName  — Deployment name for rollout restart (default: derived from imageName)
 */
def call(Map config = [:]) {
    def appDir      = config.appDir ?: error('techmekBuild: appDir is required')
    def imageName   = config.imageName ?: error('techmekBuild: imageName is required')
    def appVersion  = config.appVersion ?: '1.0.0'
    def registry    = config.registry ?: 'gitea.techmek.io'
    def registryInt = config.registryInt ?: 'gitea.gitea.svc.cluster.local'
    def dockerfile  = config.dockerfile ?: 'Dockerfile'
    def useCache    = config.cache ?: false
    def skipDeploy  = config.skipDeploy ?: false
    def namespace   = config.namespace ?: imageName.split('/')[-1]
    def deployName  = config.deployName ?: imageName.split('/')[-1]

    pipeline {
        agent none
        environment {
            REGISTRY          = registry
            REGISTRY_INTERNAL = registryInt
            IMAGE_NAME        = imageName
            APP_DIR           = appDir
            APP_VERSION       = appVersion
        }
        options {
            timeout(time: 15, unit: 'MINUTES')
            buildDiscarder(logRotator(numToKeepStr: '15'))
            timestamps()
            ansiColor('xterm')
        }
        stages {
            stage('Build & Push Image') {
                agent { label 'kaniko' }
                steps {
                    checkout scm
                    script {
                        env.GIT_COMMIT_SHORT = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                        env.BUILD_TAG_VERSION = "${appVersion}-${env.BUILD_NUMBER}-${env.GIT_COMMIT_SHORT}"
                        env.BUILD_TS = sh(script: 'date -u +%Y-%m-%dT%H:%M:%SZ', returnStdout: true).trim()
                    }
                    container('kaniko') {
                        sh """
                            /kaniko/executor \
                                --dockerfile=${appDir}/${dockerfile} \
                                --context=${appDir} \
                                --destination=${registryInt}/${imageName}:\${BUILD_TAG_VERSION} \
                                --destination=${registryInt}/${imageName}:${appVersion} \
                                --build-arg APP_VERSION=${appVersion} \
                                --build-arg BUILD_NUMBER=\${BUILD_NUMBER} \
                                --build-arg GIT_COMMIT=\${GIT_COMMIT_SHORT} \
                                --build-arg BUILD_TIMESTAMP=\${BUILD_TS} \
                                --cache=${useCache} \
                                --insecure \
                                --push-retry 3 \
                                --verbosity=info
                        """
                    }
                }
            }
            stage('Deploy') {
                when { expression { return !skipDeploy } }
                agent { label 'kubectl' }
                steps {
                    container('kubectl') {
                        sh """
                            kubectl rollout restart deployment/${deployName} -n ${namespace}
                            kubectl rollout status deployment/${deployName} -n ${namespace} --timeout=120s
                            kubectl get pods -n ${namespace} -o wide
                        """
                    }
                }
            }
        }
        post {
            success { techmekNotify(status: 'SUCCESS', app: imageName, version: env.BUILD_TAG_VERSION ?: appVersion) }
            failure { techmekNotify(status: 'FAILURE', app: imageName, version: env.BUILD_TAG_VERSION ?: appVersion) }
        }
    }
}
