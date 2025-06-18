# Shared Library Usage

## Overview

This Jenkins shared library provides reusable functions for common build and Docker operations within your Jenkins pipelines. It helps standardize these tasks and reduce boilerplate code in your Jenkinsfiles.

Current functionalities include:
- Building Java projects with Maven (`buildJar`).
- Docker operations via a `Docker` utility class:
    - Building Docker images (`buildDockerImage`).
    - Logging into Docker registries (`dockerHubLogin`).
    - Pushing Docker images (`dockerHubPush`).

## Setup

To use this shared library in your Jenkins environment:

1.  **Configure in Jenkins**:
    *   Go to "Manage Jenkins" -> "Configure System".
    *   Under "Global Pipeline Libraries", add a new library.
    *   Provide a **Name** (e.g., `my-shared-lib` or `jenkins-shared-lib` - use the name you'll reference in Jenkinsfiles).
    *   Choose a **Default version** (e.g., `main`, `master`, or a specific tag/branch).
    *   Select your **Retrieval method** (e.g., "Modern SCM").
    *   Configure your SCM details (e.g., Git repository URL for this library).

2.  **Import in Jenkinsfile**:
    At the top of your `Jenkinsfile`, import the library:

    ```groovy
    // If you named it 'my-shared-lib' in Jenkins configuration
    @Library('my-shared-lib') _

    // Alternatively, for more dynamic loading or specific versions:
    // library 'my-shared-lib@my-branch'
    ```

## Available Functions

### `buildJar`

Builds a Java project using Maven. It automatically detects the OS to use appropriate shell commands (`sh` or `bat`).

**Parameters:**

*   `config` (Map, optional): A map of configuration options.
    *   `mavenGoals` (String, optional): The Maven goals to execute. Defaults to `"clean package"`.

**Usage Examples:**

```groovy
// Build with default goals ("clean package")
buildJar()

// Build with custom Maven goals
buildJar(config: [mavenGoals: 'clean install -DskipTests'])
```

### `Docker` Utility Class

This class provides methods for various Docker operations. First, you need to create an instance of it.

**Instantiation:**

```groovy
// In your Jenkinsfile, within a script block or stage
def docker = new com.docker.Docker(this)
```
The `this` refers to the pipeline script context, providing access to Jenkins steps.

#### `docker.buildDockerImage`

Builds a Docker image.

**Parameters:**

*   `imageName` (String): The name and tag for the Docker image (e.g., 'mycompany/myimage:latest').
*   `params` (Map, optional): A map of optional parameters.
    *   `dockerfilePath` (String, optional): Path to the Dockerfile, relative to the build context. Defaults to 'Dockerfile'.
    *   `buildContext` (String, optional): The build context path for the Docker build. Defaults to '.' (current directory).

**Usage Example:**

```groovy
// Build with defaults (Dockerfile in root, context is current directory)
docker.buildDockerImage('myorg/myapp:1.0.0')

// Build with a custom Dockerfile and build context
docker.buildDockerImage('myorg/myfeature-app:latest', params: [
    dockerfilePath: 'infra/docker/Dockerfile.dev',
    buildContext: './module-app'
])
```

#### `docker.dockerHubLogin`

Logs in to a Docker registry using Jenkins credentials. Defaults to Docker Hub if no specific registry URL is given.

**Note**: This method relies on Jenkins credentials being configured with the ID `'docker-hub-credentials'`.

**Parameters:**

*   `params` (Map, optional): A map of optional parameters.
    *   `registryUrl` (String, optional): The URL of the Docker registry to log into. If omitted, login will target the default Docker Hub.

**Usage Examples:**

```groovy
// Log in to Docker Hub
docker.dockerHubLogin()

// Log in to a custom private registry
docker.dockerHubLogin(params: [registryUrl: 'https://my.private-registry.example.com'])
```

#### `docker.dockerHubPush`

Pushes a Docker image to a Docker registry.

**Parameters:**

*   `imageName` (String): The full name of the Docker image to push.
    *   For non-Docker Hub registries, this name **MUST** be fully qualified (e.g., 'my.registry.com/myuser/myimage:tag').
    *   For Docker Hub, it can be 'username/imagename:tag'.
*   `params` (Map, optional): A map of optional parameters.
    *   `registryUrl` (String, optional): The URL of the target Docker registry. This is primarily used for logging purposes to clarify intent, as the actual push destination is derived from the `imageName`.

**Usage Examples:**

```groovy
// Push to Docker Hub (imageName is like 'username/image:tag')
docker.dockerHubPush('myusername/myfirstapp:1.0')

// Push to a custom private registry
// imageName must be fully qualified
docker.dockerHubPush('my.private-registry.example.com/myusername/myfirstapp:1.0', params: [
    registryUrl: 'my.private-registry.example.com' // For logging clarity
])
```
