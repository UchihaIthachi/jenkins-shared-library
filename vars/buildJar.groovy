#!/usr/bin/env groovy

/**
 * Builds a Java project using Maven.
 * This script executes Maven goals to compile, test, and package the project.
 * It supports different execution commands based on the operating system (Unix/Windows)
 * and allows customization of Maven goals.
 *
 * @param config A map of configuration options.
 *               - mavenGoals (String, optional): The Maven goals to execute. Defaults to "clean package".
 *
 * Example usage in Jenkinsfile:
 * <code>
 * // Build with default goals (clean package)
 * buildJar()
 *
 * // Build with custom goals
 * buildJar(config: [mavenGoals: "clean install -DskipTests"])
 *
 * // Another example with different custom goals
 * buildJar(mavenGoals: "verify") // Direct argument passing also works for vars
 * </code>
 */
def call(Map config = [:]) {
    def mavenGoals = config.mavenGoals ?: "clean package"
    echo "Building jar artifact for $GIT_BRANCH with goals: '${mavenGoals}'" // GIT_BRANCH - reference to the environment variable
    try {
        if (isUnix()) {
            sh "mvn ${mavenGoals}"
        } else {
            bat "mvn ${mavenGoals}"
        }
        echo "Maven build with goals '${mavenGoals}' successful for branch $GIT_BRANCH."
    } catch (Exception e) {
        echo "Caught exception during Maven build (goals: '${mavenGoals}') for branch $GIT_BRANCH: ${e}"
        error "Error during Maven build (goals: '${mavenGoals}') in buildJar.groovy for branch $GIT_BRANCH: ${e.getMessage()}"
    }
}