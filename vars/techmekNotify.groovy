/**
 * techmekNotify — Send build notifications via Telegram.
 *
 * Usage:
 *   techmekNotify(status: 'SUCCESS', app: 'admin/myapp', version: '1.0.0-5-abc123')
 *   techmekNotify(status: 'FAILURE', app: 'admin/myapp', message: 'Health check timed out')
 *
 * Parameters:
 *   status  — Build status: SUCCESS, FAILURE, STARTED, ABORTED (required)
 *   app     — Application/image name (required)
 *   version — Build version string (optional)
 *   message — Custom message body (optional, overrides default)
 */
def call(Map config = [:]) {
    def status  = config.status ?: 'UNKNOWN'
    def app     = config.app ?: 'unknown'
    def version = config.version ?: env.BUILD_TAG_VERSION ?: 'N/A'
    def message = config.message ?: null

    def emoji = [
        SUCCESS: '\u2705',
        FAILURE: '\u274C',
        STARTED: '\u25B6\uFE0F',
        ABORTED: '\u23F9\uFE0F',
    ].getOrDefault(status, '\u2753')

    def defaultMsg = """${emoji} *Jenkins Build ${status}*
App: `${app}`
Version: `${version}`
Build: [#${env.BUILD_NUMBER}](${env.BUILD_URL})
Branch: `${env.BRANCH_NAME ?: env.GIT_BRANCH ?: 'N/A'}`"""

    def text = message ?: defaultMsg

    withCredentials([string(credentialsId: 'telegram-bot-token', variable: 'BOT_TOKEN')]) {
        def chatId = '357919125'
        def payload = groovy.json.JsonOutput.toJson([
            chat_id: chatId,
            text: text,
            parse_mode: 'Markdown',
            disable_web_page_preview: true,
        ])
        httpRequest(
            url: "https://api.telegram.org/bot${BOT_TOKEN}/sendMessage",
            httpMode: 'POST',
            contentType: 'APPLICATION_JSON',
            requestBody: payload,
            validResponseCodes: '200:299',
            quiet: true,
        )
    }
}
