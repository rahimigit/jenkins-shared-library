/**
 * techmekDeploy — GitOps deployment step (update image tag → ArgoCD auto-sync).
 *
 * Usage:
 *   techmekDeploy(namespace: 'appisup', deployment: 'appisup', rollout: true)
 *
 * Parameters:
 *   namespace   — Kubernetes namespace (required)
 *   deployment  — Deployment name (required)
 *   rollout     — Wait for rollout completion (default: true)
 *   timeout     — Rollout timeout in seconds (default: 120)
 */
def call(Map config = [:]) {
    def namespace  = config.namespace ?: error('techmekDeploy: namespace is required')
    def deployment = config.deployment ?: error('techmekDeploy: deployment is required')
    def rollout    = config.rollout != null ? config.rollout : true
    def timeout    = config.timeout ?: 120

    container('kubectl') {
        sh """
            echo "=== Restarting deployment ${deployment} in ${namespace} ==="
            kubectl rollout restart deployment/${deployment} -n ${namespace}
        """
        if (rollout) {
            sh """
                echo "=== Waiting for rollout (timeout ${timeout}s) ==="
                kubectl rollout status deployment/${deployment} -n ${namespace} --timeout=${timeout}s
                echo "=== Pod Status ==="
                kubectl get pods -n ${namespace} -o wide
            """
        }
    }
}
