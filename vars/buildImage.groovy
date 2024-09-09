#!/usr/bin/env groovy

import com.docker.Docker

def call(String imageName) {
    return new Docker(this).buildDockerImage(imageName)
}