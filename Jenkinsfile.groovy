@Library('semantic_releasing') _

podTemplate(label: 'mypod', containers: [
        containerTemplate(name: 'docker', image: 'docker', ttyEnabled: true, command: 'cat'),
        containerTemplate(name: 'kubectl', image: 'lachlanevenson/k8s-kubectl:v1.8.0', command: 'cat', ttyEnabled: true),
        containerTemplate(name: 'maven', image: 'maven:3.5.2-jdk-8', command: 'cat', ttyEnabled: true)
],
        volumes: [
                hostPathVolume(mountPath: '/var/run/docker.sock', hostPath: '/var/run/docker.sock'),
        ]) {
    node('mypod') {

        stage('checkout & unit tests & build') {
            git url: 'https://github.com/maesi/ch-open-18'
            container('maven') {
                sh 'mvn clean package'
            }
            junit allowEmptyResults: true, testResults: '**/target/surefire-reports/TEST-*.xml'
        }

        stage('build image & git tag & docker push') {
            env.VERSION = semanticReleasing()
            currentBuild.displayName = env.VERSION
            container('maven') {
                sh "mvn versions:set -DnewVersion=${env.VERSION}"
            }

            sh "git config user.email \"jenkins@khinkali.ch\""
            sh "git config user.name \"Jenkins\""
            sh "git tag -a ${env.VERSION} -m \"${env.VERSION}\""
            withCredentials([usernamePassword(credentialsId: 'github', passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
                sh "git push https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/maesi/ch-open-18.git --tags"
            }

            container('docker') {
                sh "docker build -t maesi/ch-open-18:${env.VERSION} ."
                withCredentials([usernamePassword(credentialsId: 'dockerhub_maesi', passwordVariable: 'DOCKER_PASSWORD', usernameVariable: 'DOCKER_USERNAME')]) {
                    sh "docker login --username ${DOCKER_USERNAME} --password ${DOCKER_PASSWORD}"
                }
                sh "docker push maesi/ch-open-18:${env.VERSION}"
            }
        }

        stage('deploy to test') {
            sh "sed -i -e 's~image: maesi/ch-open-18:0.0.1~image: maesi/ch-open-18:${env.VERSION}~' deployment.yml"
            container('kubectl') {
                sh "kubectl apply -f deployment.yml"
            }
        }
    }
}