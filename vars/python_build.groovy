def call(dockerRepoName, imageName) {
    pipeline { 
        agent any  
        stages { 
            stage('Build') { 
                steps { 
                    sh 'pip install -r ${dockerRepoName}/requirements.txt' 
                } 
            } 
            stage('Python Lint') { 
                steps { 
                    sh 'pylint-fail-under --fail_under 5.0 ${dockerRepoName}/*.py' 
                } 
            }
            stage('Zip Artifacts') {
                steps {
                    script {
                        zip archive: true, dir: '', glob: '*.py', zipFile: '${dockerRepoName}_app.zip', overwrite: true
                    }
                    archiveArtifacts artifacts: 'app.zip', fingerprint: true
            }
        }
            stage('Package') { 
                when { 
                    expression { env.GIT_BRANCH == 'origin/main' } 
                } 
                steps { 
                    withCredentials([string(credentialsId: 'DockerHub', variable: 'TOKEN')]) { 
                    sh "docker login -u 'rortega4' -p '$TOKEN' docker.io" 
                    sh "docker build -t ${dockerRepoName}:latest --tag rortega4/${dockerRepoName}:${imageName} ." 
                    sh "docker push rortega4/${dockerRepoName}:${imageName}" 
                } 
            } 
        } 
    }
}
}
