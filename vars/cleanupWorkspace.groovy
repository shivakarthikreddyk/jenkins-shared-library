def call() {

    sh 'docker compose down -v --remove-orphans || true'

    cleanWs(
        deleteDirs: true,
        notFailBuild: true
    )
}