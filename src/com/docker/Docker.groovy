#!/usr/bin/env groovy

package com.docker

class Docker implements Serializable {

    def script

    Docker(script) {
        this.script = script
    }

    def isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win")
    }

    def buildDockerImage(String imageName) {
        script.echo "Building docker image"

        if (isWindows()) {
            // Windows specific command
            script.bat "docker build -t ${imageName} ."
        } else {
            // Linux / Unix specific command
            script.sh "docker build -t ${imageName} ."
        }
    }

    def dockerHubLogin() {
        script.echo "Logging in to Docker Hub"

        script.withCredentials([script.usernamePassword(
                credentialsId: 'docker-hub-credentials',
                passwordVariable: 'DOCKER_HUB_PASSWORD',
                usernameVariable: 'DOCKER_HUB_USERNAME')]) {

            if (isWindows()) {
                // Windows specific command
                script.bat "echo ${script.DOCKER_HUB_PASSWORD} | docker login -u ${script.DOCKER_HUB_USERNAME} --password-stdin"
            } else {
                // Linux / Unix specific command
                script.sh "echo '${script.DOCKER_HUB_PASSWORD}' | docker login -u '${script.DOCKER_HUB_USERNAME}' --password-stdin"
            }
        }
    }

    def dockerHubPush(String imageName) {
        script.echo "Pushing docker image to Docker Hub"

        if (isWindows()) {
            // Windows specific command
            script.bat "docker push ${imageName}"
        } else {
            // Linux / Unix specific command
            script.sh "docker push ${imageName}"
        }
    }
}
