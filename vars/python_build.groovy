def call(dockerRepoName, imageName) {
    pipeline { 
        agent any  
        stages { 
            stage('Build') { 
                steps { 
                    echo "${dockerRepoName} test"
                    sh "pip install -r ${dockerRepoName}/requirements.txt" 
                } 
            } 
            stage('Python Lint') { 
                steps { 
                    sh "pylint-fail-under --fail_under 5.0 ${dockerRepoName}/*.py" 
                } 
            }
            stage('Zip Artifacts') {
                steps {
                    script {
                        zip archive: true, dir: '', glob: '*.py', zipFile: "${dockerRepoName}_app.zip", overwrite: true
                    }
                    archiveArtifacts artifacts: "${dockerRepoName}_app.zip", fingerprint: true
            }
        }
            stage('Package') { 
                when { 
                    expression { env.GIT_BRANCH == 'origin/main' } 
                } 
                steps { 
                    withCredentials([string(credentialsId: 'DockerHub', variable: 'TOKEN')]) { 
                    sh "docker login -u 'rortega4' -p '$TOKEN' docker.io" 
                    sh "docker build -t ${dockerRepoName}:latest --tag rortega4/${dockerRepoName}:${imageName} ${dockerRepoName}" 
                    sh "docker push rortega4/${dockerRepoName}:${imageName}" 
                } 
            } 
        }
            stage('Scan Image') {
                steps {
                    withCredentials([string(credentialsId: 'SnykAuthToken', variable: 'TOKEN')]) {
                    sh "docker scan --accept-license --login --token $TOKEN"
                    sh "docker scan --accept-license --severity high rortega4/${dockerRepoName}:${imageName}"
               }
            }
        } 
    }
  }
}
