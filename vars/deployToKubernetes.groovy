def call(Map config = [:]) {

    String credentialsId =
        config.credentialsId ?: 'docker-desktop-kubeconfig'

    String namespace =
        config.namespace ?: 'sakila'

    String manifestDirectory =
        config.manifestDirectory ?: 'k8s'

    String backendImage = config.backendImage
    String frontendImage = config.frontendImage
    String imageTag = config.imageTag ?: env.BUILD_NUMBER

    withKubeConfig([
        credentialsId: credentialsId
    ]) {

        sh """
            kubectl create namespace ${namespace} \
                --dry-run=client \
                -o yaml | kubectl apply -f -
        """

        sh """
            kubectl apply \
                --namespace ${namespace} \
                --filename ${manifestDirectory}
        """

        if (backendImage) {
            sh """
                kubectl set image \
                    deployment/sakila-backend \
                    backend=${backendImage}:${imageTag} \
                    --namespace ${namespace}
            """
        }

        if (frontendImage) {
            sh """
                kubectl set image \
                    deployment/sakila-frontend \
                    frontend=${frontendImage}:${imageTag} \
                    --namespace ${namespace}
            """
        }
    }
}