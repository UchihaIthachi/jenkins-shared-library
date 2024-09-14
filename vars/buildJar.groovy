#!/usr/bin/env groovy

def isWindows() {
    return env.OS?.toLowerCase().contains("windows")
}

def call() {
    echo "Building jar artifact for $GIT_BRANCH" // Reference to the environment variable

    if (isWindows()) {
        // Windows-specific command
        bat "mvn clean package"
    } else {
        // Linux/Unix-specific command
        sh "mvn clean package"
    }
}
