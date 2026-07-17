def call(Map config = [:]) {

    String credentialsId =
        config.credentialsId ?: 'dockerhub-credentials'

    String registry =
        config.registry ?: 'https://index.docker.io/v1/'

    String backendImage = config.backendImage
    String frontendImage = config.frontendImage
    String imageTag = config.imageTag ?: env.BUILD_NUMBER

    if (!backendImage || !frontendImage) {
        error 'backendImage and frontendImage are required.'
    }

    docker.withRegistry(registry, credentialsId) {

        docker.image("${backendImage}:${imageTag}").push()
        docker.image("${backendImage}:${imageTag}").push('latest')

        docker.image("${frontendImage}:${imageTag}").push()
        docker.image("${frontendImage}:${imageTag}").push('latest')
    }
}