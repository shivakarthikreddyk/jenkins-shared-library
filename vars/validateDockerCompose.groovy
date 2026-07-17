def call(Map config = [:]) {

    int timeoutSeconds = config.timeoutSeconds ?: 180

    try {

        sh 'docker compose down -v --remove-orphans || true'

        sh 'docker compose up -d --build'

        timeout(time: timeoutSeconds, unit: 'SECONDS') {

            waitUntil {

                int status = sh(
                    script: '''
                        curl --silent \
                             --fail \
                             http://localhost:5000/api/health \
                             > /dev/null
                    ''',
                    returnStatus: true
                )

                if (status != 0) {
                    sleep 5
                }

                return status == 0
            }
        }

        sh 'curl --fail http://localhost:5000/api/health'
        sh 'curl --fail http://localhost:3000'

        sh 'docker compose ps'

    } finally {

        sh '''
            docker compose logs --no-color \
            > docker-compose.log || true
        '''

        archiveArtifacts(
            artifacts: 'docker-compose.log',
            allowEmptyArchive: true
        )

        sh 'docker compose down -v --remove-orphans || true'
    }
}