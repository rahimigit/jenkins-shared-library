package com.techmek

/**
 * TechMek shared configuration constants for Jenkins pipelines.
 * Centralizes registry URLs, namespaces, and infrastructure values.
 */
class Config implements Serializable {

    static final String REGISTRY = 'gitea.techmek.io'
    static final String REGISTRY_INTERNAL = 'gitea.gitea.svc.cluster.local'
    static final String DOMAIN = 'techmek.io'

    static final String TELEGRAM_CHAT_ID = '357919125'
    static final String TELEGRAM_BOT_CREDENTIAL = 'telegram-bot-token'

    static final String GITHUB_CREDENTIAL = 'github-token'
    static final String GITEA_CREDENTIAL = 'gitea-token'

    /** Map Git branch patterns to deployment environments. */
    static String branchToEnvironment(String branch) {
        if (branch ==~ /^v\d+.*/ || branch == 'release') return 'production'
        if (branch == 'main' || branch == 'master') return 'staging'
        if (branch ==~ /^dev.*/ || branch ==~ /^feature.*/) return 'dev'
        if (branch ==~ /^PR-\d+/) return 'test'
        return 'dev'
    }

    /** Standard labels for all pod templates. */
    static Map<String, String> podLabels(String app) {
        return [
            'app.kubernetes.io/managed-by': 'jenkins',
            'app.kubernetes.io/part-of': 'ci-cd',
            'app.kubernetes.io/name': app,
        ]
    }
}
