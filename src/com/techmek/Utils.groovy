package com.techmek

/**
 * Utility functions for TechMek Jenkins pipelines.
 * Adapted from FuchiCorp's CommonFunction.groovy for OCI/Gitea/k3s stack.
 */
class Utils implements Serializable {

    /**
     * Get the user who triggered the build.
     * Returns 'timer' for scheduled builds, 'scm' for webhook triggers.
     */
    static String getBuildUser(def script) {
        try {
            def cause = script.currentBuild.rawBuild?.getCauses()?.find {
                it instanceof hudson.model.Cause.UserIdCause
            }
            return cause?.userId ?: 'automated'
        } catch (Exception e) {
            return 'automated'
        }
    }

    /**
     * Pretty-print a JSON string.
     */
    static String prettyJson(String raw) {
        try {
            def parsed = new groovy.json.JsonSlurper().parseText(raw)
            return groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(parsed))
        } catch (Exception e) {
            return raw
        }
    }

    /**
     * Generate a build version tag: <appVersion>-<buildNumber>-<gitShort>
     */
    static String buildTag(def script, String appVersion) {
        def gitShort = script.sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
        return "${appVersion}-${script.env.BUILD_NUMBER}-${gitShort}"
    }

    /**
     * Check if Gitea container registry has a specific image tag.
     */
    static boolean imageExists(def script, String imageName, String tag) {
        def result = script.sh(
            script: """
                curl -sf -o /dev/null \
                    -u "\${GITEA_USER}:\${GITEA_TOKEN}" \
                    "https://${Config.REGISTRY}/v2/${imageName}/manifests/${tag}"
            """,
            returnStatus: true,
        )
        return result == 0
    }
}
