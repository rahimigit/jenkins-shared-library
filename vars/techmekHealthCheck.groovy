/**
 * techmekHealthCheck — Post-deploy health check via kubectl exec.
 *
 * Usage:
 *   techmekHealthCheck(namespace: 'appisup', deployment: 'appisup', url: 'http://localhost:8000/healthz')
 *
 * Parameters:
 *   namespace  — Kubernetes namespace (required)
 *   deployment — Deployment name (required)
 *   url        — Health check URL inside the pod (required)
 *   retries    — Number of check attempts (default: 12)
 *   interval   — Seconds between retries (default: 5)
 */
def call(Map config = [:]) {
    def namespace  = config.namespace ?: error('techmekHealthCheck: namespace is required')
    def deployment = config.deployment ?: error('techmekHealthCheck: deployment is required')
    def url        = config.url ?: error('techmekHealthCheck: url is required')
    def retries    = config.retries ?: 12
    def interval   = config.interval ?: 5

    container('kubectl') {
        sh """
            echo "=== Running health check: ${url} ==="
            for i in \$(seq 1 ${retries}); do
                STATUS=\$(kubectl exec -n ${namespace} deploy/${deployment} -- \
                    python3 -c "import urllib.request; print(urllib.request.urlopen('${url}').read().decode())" 2>/dev/null || echo "NOT_READY")
                if echo "\$STATUS" | grep -qi "ok\\|healthy\\|running"; then
                    echo "Health check passed on attempt \$i!"
                    exit 0
                fi
                echo "Attempt \$i/${retries}: Not ready yet..."
                sleep ${interval}
            done
            echo "Health check FAILED after ${retries} attempts"
            exit 1
        """
    }
}
