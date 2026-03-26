# TechMek Jenkins Shared Library

Shared library for TechMek Jenkins CI/CD pipelines running on a k3s cluster with Gitea registry, Kaniko builds, and ArgoCD GitOps deployments.

## Usage

In your `Jenkinsfile`:
```groovy
@Library('techmek-shared-library') _

// Full pipeline (build + deploy + health check + notifications)
techmekBuild(
    appDir: 'gitops/apps/myapp',
    imageName: 'admin/myapp',
    appVersion: '2.0.0'
)
```

Or use individual steps:
```groovy
@Library('techmek-shared-library') _

pipeline {
    agent none
    stages {
        stage('Build') {
            agent { label 'kaniko' }
            steps {
                // ... Kaniko build ...
            }
        }
        stage('Deploy') {
            agent { label 'kubectl' }
            steps {
                techmekDeploy(namespace: 'myapp', deployment: 'myapp')
                techmekHealthCheck(namespace: 'myapp', deployment: 'myapp', url: 'http://localhost:8080/health')
            }
        }
    }
    post {
        success { techmekNotify(status: 'SUCCESS', app: 'myapp') }
        failure { techmekNotify(status: 'FAILURE', app: 'myapp') }
    }
}
```

## Available Functions

| Function | Purpose |
|----------|---------|
| `techmekBuild()` | Full Kaniko build → deploy → notify pipeline |
| `techmekNotify()` | Send Telegram build notifications |
| `techmekDeploy()` | Restart deployment and wait for rollout |
| `techmekHealthCheck()` | Post-deploy health check via kubectl exec |
| `techmekApproval()` | Production deployment approval gate |

## Classes

| Class | Package | Purpose |
|-------|---------|---------|
| `Config` | `com.techmek` | Shared constants (registry, credentials, branch mapping) |
| `Utils` | `com.techmek` | Build user detection, JSON helper, image existence check |

## Infrastructure

- **Registry**: `gitea.techmek.io` (internal: `gitea.gitea.svc.cluster.local`)
- **Build**: Kaniko in-cluster (no Docker socket)
- **Deploy**: ArgoCD auto-sync (kubectl rollout restart triggers new image pull)
- **Notifications**: Telegram bot (`@ordinclaw_bot`)
- **Auth**: Authentik OIDC
