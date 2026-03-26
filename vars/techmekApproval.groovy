/**
 * techmekApproval — Production deployment approval gate.
 *
 * Usage:
 *   techmekApproval(env: 'production', timeout: 30)
 *
 * Parameters:
 *   env     — Target environment name (required)
 *   timeout — Approval timeout in minutes (default: 30)
 *   message — Custom approval message (optional)
 */
def call(Map config = [:]) {
    def targetEnv = config.env ?: 'production'
    def timeoutMin = config.timeout ?: 30
    def message = config.message ?: "Approve deployment to ${targetEnv}?"

    if (targetEnv == 'production' || targetEnv == 'prod') {
        techmekNotify(
            status: 'STARTED',
            app: env.JOB_NAME ?: 'unknown',
            message: "\u26A0\uFE0F *Approval Required*\nJob: `${env.JOB_NAME}`\nEnvironment: `${targetEnv}`\n[Approve](${env.BUILD_URL}input/)"
        )
    }

    timeout(time: timeoutMin, unit: 'MINUTES') {
        input(
            message: message,
            ok: "Deploy to ${targetEnv}",
            submitterParameter: 'approver',
        )
    }
}
