#!/usr/bin/env groovy

def call() {
    echo "Building jar artifact for $GIT_BRANCH" // GIT_BRANCH - reference to the environment variable
    sh "mvn clean package"
}