def call(Map config = [:]) {

    String backendImage = config.backendImage
    String frontendImage = config.frontendImage
    String imageTag = config.imageTag ?: env.BUILD_NUMBER

    if (!backendImage || !frontendImage) {
        error 'backendImage and frontendImage are required.'
    }

    echo "Building backend image: ${backendImage}:${imageTag}"

    sh """
        docker build \
          --tag ${backendImage}:${imageTag} \
          --tag ${backendImage}:latest \
          ./backend
    """

    echo "Building frontend image: ${frontendImage}:${imageTag}"

    sh """
        docker build \
          --tag ${frontendImage}:${imageTag} \
          --tag ${frontendImage}:latest \
          ./frontend
    """

    sh """
        docker image inspect ${backendImage}:${imageTag}
        docker image inspect ${frontendImage}:${imageTag}
    """
}