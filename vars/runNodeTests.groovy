def call(Map config = [:]) {

    String directory = config.directory ?: '.'

    boolean allowMissingTests =
        config.get('allowMissingTests', true)

    dir(directory) {

        if (!fileExists('package.json')) {
            echo "No package.json found in ${directory}"
            return
        }

        sh 'node --version'
        sh 'npm --version'

        if (fileExists('package-lock.json')) {
            sh 'npm ci'
        } else {
            sh 'npm install'
        }

        if (allowMissingTests) {
            sh 'npm test --if-present'
        } else {
            sh 'npm test'
        }
    }
}