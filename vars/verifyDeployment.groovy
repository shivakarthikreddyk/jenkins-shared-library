def call(Map config = [:]) {

    String credentialsId =
        config.credentialsId ?: 'docker-desktop-kubeconfig'

    String namespace =
        config.namespace ?: 'sakila'

    int rolloutTimeout =
        config.rolloutTimeout ?: 180

    withKubeConfig([
        credentialsId: credentialsId
    ]) {

        sh """
            kubectl rollout status \
                deployment/sakila-backend \
                --namespace ${namespace} \
                --timeout=${rolloutTimeout}s
        """

        sh """
            kubectl rollout status \
                deployment/sakila-frontend \
                --namespace ${namespace} \
                --timeout=${rolloutTimeout}s
        """

        sh """
            kubectl get pods \
                --namespace ${namespace} \
                --output wide
        """

        sh """
            kubectl get services \
                --namespace ${namespace}
        """

        sh """
            kubectl get deployments \
                --namespace ${namespace}
        """

        sh '''
            curl --retry 10 \
                 --retry-delay 5 \
                 --fail \
                 http://host.docker.internal:30080/api/health
        '''

        sh '''
            curl --retry 10 \
                 --retry-delay 5 \
                 --fail \
                 http://host.docker.internal:30081
        '''
    }
}