#!/usr/bin/env groovy

import com.docker.Docker

def call() {
    return new Docker(this).dockerHubLogin()
}