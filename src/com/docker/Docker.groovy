#!/usr/bin/env groovy

package com.docker

// Implements serializable interface because we need to save the state of the execution if the pipeline is paused / resumed
class Docker implements Serializable {

    def script

    Docker(script) {
        this.script = script
    }

    def buildDockerImage(String imageName) {

        script.echo "Building docker image"

        // Execute a shell command to build a Docker image
        script.sh "docker build -t $imageName ."

    }

    def dockerHubLogin() {

        script.echo "Logging in to Docker Hub"

        script.withCredentials([script.usernamePassword(
                credentialsId: 'docker-hub-credentials',
                passwordVariable: 'DOCKER_HUB_PASSWORD',
                usernameVariable: 'DOCKER_HUB_USERNAME')]) {

            // Execute a shell command to log in to Docker Hub
            // The `echo $DOCKER_HUB_PASSWORD` part is used to pass the password to the `docker login` command
            // The `--password-stdin` option tells Docker to read the password from the standard input
            script.sh "echo '${script.DOCKER_HUB_PASSWORD}' | docker login -u '${script.DOCKER_HUB_USERNAME}' --password-stdin"


        }
    }

    def dockerHubPush(String imageName) {

        script.echo "Pushing docker image to Docker Hub"

        // Execute a shell command to push the Docker image to Docker Hub
        script.sh "docker push $imageName"
    }

}