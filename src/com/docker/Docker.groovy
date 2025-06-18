#!/usr/bin/env groovy

package com.docker

// Implements serializable interface because we need to save the state of the execution if the pipeline is paused / resumed
class Docker implements Serializable {

    def script

    /**
     * Constructs a new Docker utility object.
     * This object provides methods for interacting with Docker (build, login, push).
     * It requires a reference to the Jenkins pipeline script environment to execute pipeline steps.
     *
     * @param script The Jenkins pipeline script object, providing access to pipeline steps
     *               like sh, bat, echo, withCredentials, error, etc. This is typically 'this'
     *               when instantiating from a pipeline script (e.g., `def docker = new com.docker.Docker(this)`).
     */
    Docker(script) {
        this.script = script
    }

    /**
     * Builds a Docker image using the 'docker build' command.
     * It allows specifying the image name/tag, Dockerfile location, and build context.
     *
     * @param imageName The name and tag for the Docker image (e.g., 'mycompany/myimage:latest', 'myimage:1.0').
     * @param params A map of optional parameters.
     *               - dockerfilePath (String, optional): Path to the Dockerfile. Defaults to 'Dockerfile' in the root of the build context.
     *               - buildContext (String, optional): The build context path for the Docker build. Defaults to '.' (current directory).
     *
     * Example usage:
     * <code>
     * def docker = new com.docker.Docker(this) // Assuming 'this' is the pipeline script scope
     *
     * // Build with default Dockerfile ('./Dockerfile') and context ('.')
     * docker.buildDockerImage('myorganization/my-app:1.0.0')
     *
     * // Build with a specific Dockerfile and build context
     * docker.buildDockerImage('myorganization/my-app:latest', params: [dockerfilePath: 'dockerfiles/Dockerfile.prod', buildContext: './src'])
     *
     * // Build using a Dockerfile in the parent directory and current directory as context
     * docker.buildDockerImage('another/image:SNAPSHOT', dockerfilePath: '../Dockerfile.dev', buildContext: '.') // Direct named arguments
     * </code>
     */
    def buildDockerImage(String imageName, Map params = [:]) {
        def dockerfilePath = params.dockerfilePath ?: 'Dockerfile'
        def buildContext = params.buildContext ?: '.'
        script.echo "Building docker image: ${imageName} using Dockerfile: ${dockerfilePath} from context: ${buildContext}"
        try {
            if (script.isUnix()) {
                script.sh "docker build -t \"${imageName}\" -f \"${dockerfilePath}\" \"${buildContext}\""
            } else {
                script.bat "docker build -t \"${imageName}\" -f \"${dockerfilePath}\" \"${buildContext}\""
            }
            script.echo "Docker image ${imageName} built successfully using Dockerfile: ${dockerfilePath}."
        } catch (Exception e) {
            script.echo "Caught exception in buildDockerImage (image: ${imageName}, Dockerfile: ${dockerfilePath}, context: ${buildContext}): ${e}"
            script.error "Error in buildDockerImage for ${imageName} (Dockerfile: ${dockerfilePath}, context: ${buildContext}) in Docker.groovy: ${e.getMessage()}"
        }
    }

    /**
     * Logs in to a Docker registry using credentials stored in Jenkins.
     * By default, this method attempts to log in to Docker Hub.
     * A custom registry URL can be specified via the `params` map.
     *
     * Note: This method relies on Jenkins credentials being configured with the ID 'docker-hub-credentials'.
     * Ensure these credentials (username and password) are correctly set up in Jenkins.
     *
     * @param params A map of optional parameters.
     *               - registryUrl (String, optional): The URL of the Docker registry to log into.
     *                                                If omitted or an empty string is provided,
     *                                                the login will target the default Docker Hub.
     *
     * Example usage:
     * <code>
     * def docker = new com.docker.Docker(this)
     *
     * // Log in to Docker Hub (default behavior)
     * docker.dockerHubLogin()
     *
     * // Log in to a custom private registry
     * docker.dockerHubLogin(params: [registryUrl: 'my.private-registry.com:5000'])
     *
     * // Log in to another custom registry using direct named argument
     * docker.dockerHubLogin(registryUrl: 'another.registry.io')
     * </code>
     */
    def dockerHubLogin(Map params = [:]) {
        def registryUrl = params.registryUrl
        def targetRegistryDesc = (registryUrl && !registryUrl.trim().isEmpty()) ? "registry ${registryUrl.trim()}" : "default Docker Hub"

        script.echo "Logging in to ${targetRegistryDesc}"
        try {
            script.withCredentials([script.usernamePassword(
                credentialsId: 'docker-hub-credentials', // Remains for this iteration
                passwordVariable: 'DOCKER_HUB_PASSWORD',
                usernameVariable: 'DOCKER_HUB_USERNAME')]) {

                script.echo "Attempting login to ${targetRegistryDesc} using configured credentials."

                def finalRegistryUrl = (registryUrl && !registryUrl.trim().isEmpty()) ? registryUrl.trim() : null

                if (script.isUnix()) {
                    def command = "docker login --username=\${script.DOCKER_HUB_USERNAME} --password=\${script.DOCKER_HUB_PASSWORD}"
                    if (finalRegistryUrl) {
                        command += " ${finalRegistryUrl}"
                    }
                    script.sh command
                } else {
                    def command = "docker login --username=%DOCKER_HUB_USERNAME% --password=%DOCKER_HUB_PASSWORD%"
                    if (finalRegistryUrl) {
                        command += " ${finalRegistryUrl}"
                    }
                    script.bat command
                }
                script.echo "Docker login successful to ${targetRegistryDesc} using configured credentials."
            }
        } catch (Exception e) {
            script.echo "Caught exception during login to ${targetRegistryDesc}: ${e}"
            script.error "Error during Docker login to ${targetRegistryDesc}: ${e.getMessage()}"
        }
    }

    /**
     * Pushes a Docker image to a Docker registry using the 'docker push' command.
     *
     * @param imageName The full name of the Docker image to push.
     *                  This name must be fully qualified (e.g., 'my.registry.com/myuser/myimage:tag' or 'myregistry/myimage:latest')
     *                  if pushing to a specific/private registry. For Docker Hub, it can be 'username/imagename:tag'.
     *                  The actual destination of the push is determined by this image name.
     * @param params A map of optional parameters.
     *               - registryUrl (String, optional): The URL of the target Docker registry.
     *                                                This parameter is primarily used for logging purposes to provide clarity
     *                                                on the intended destination. The 'imageName' itself dictates where the image is pushed.
     *                                                If provided, it should match the registry part of the 'imageName'.
     *
     * Example usage:
     * <code>
     * def docker = new com.docker.Docker(this)
     *
     * // Push to Docker Hub (imageName should be e.g., 'yourdockerhubuser/app:1.0')
     * docker.dockerHubPush('yourdockerhubuser/my-app:1.2.3')
     *
     * // Push to a custom private registry
     * // imageName must be fully qualified: 'my.privateregistry.com/namespace/my-app:1.2.3'
     * docker.dockerHubPush('my.privateregistry.com/namespace/my-app:1.2.3', params: [registryUrl: 'my.privateregistry.com'])
     *
     * // Push to another custom registry using direct named argument for logging clarity
     * docker.dockerHubPush('another.registry.io/another-app:latest', registryUrl: 'another.registry.io')
     * </code>
     */
    def dockerHubPush(String imageName, Map params = [:]) {
        def registryUrl = params.registryUrl
        def targetRegistryDesc = (registryUrl && !registryUrl.trim().isEmpty()) ? "registry ${registryUrl.trim()}" : "default Docker Hub (or registry implied by image name)"

        script.echo "Pushing docker image ${imageName} to ${targetRegistryDesc}"
        try {
            if (script.isUnix()) {
                script.sh "docker push \"${imageName}\""
            } else {
                script.bat "docker push \"${imageName}\""
            }
            script.echo "Docker image ${imageName} pushed successfully to ${targetRegistryDesc}."
        } catch (Exception e) {
            script.echo "Caught exception in dockerHubPush for ${imageName} to ${targetRegistryDesc}: ${e}"
            script.error "Error in dockerHubPush for ${imageName} to ${targetRegistryDesc}: ${e.getMessage()}"
        }
    }

}