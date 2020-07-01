// A Jenkinsfile useful for Verification. Will run UnitTests+Sonar and IntegrationTests for the project
def sonarUrl = 'http://localhost:9000/'
def sonarToken = '4c862f93839d8f4c88adea637d59d91967e8d5c7'

pipeline {
  agent none
  parameters {
    string(name: 'profile', defaultValue: 'Jenkins', description: 'Maven profiles to be used when running maven build')
    string(name: 'mavenRepository', defaultValue: 'http://localhost:8081/repository/maven-public/', description: 'Remote Maven Repository')
    booleanParam(name: 'parallel', defaultValue: true, description: 'Run mvn in Parallel')
  }
  stages {
    stage('Local Build') {
      agent { label 'master' }
      steps {
        script { // https://issues.jenkins-ci.org/browse/JENKINS-41929
          // On first build by user just load the parameters as they are not available of first run on new branches
          if (env.BUILD_NUMBER.equals("1") && currentBuild.getBuildCauses('hudson.model.Cause$UserIdCause') != null) {
            currentBuild.displayName = 'Parameter loading'
            currentBuild.result = 'ABORTED'
            error('Stopping initial manually triggered build as we only want to get the parameters')
          }
        }

        withDockerContainer(
            image: 'maven:3-jdk-8',
            args: '--net="host" -e MAVEN_REMOTE_REPOSITORY='+ params.mavenRepository,
            toolName: env.DOCKER_TOOL_NAME
        ) {
          script {
            // this settings.xml assumes there is an environment variable called 'MAVEN_REMOTE_REPOSITORY'
            // which already exists on the jenkins docker
            // I tried to put this file inside my docker images, but somehow on Windows machine, it
            // can not be mounted... Using Gitlab would required authentication so I just used this
            // file which is hosted on github.
            sh 'curl -o /tmp/settings.xml https://raw.githubusercontent.com/amirmv2006/build-jenk/sandbox/generic-maven-settings.xml'
            echo 'Using this settings.xml:'
            sh 'cat /tmp/settings.xml'
            def parallelParam = ''
            if (params.parallel) {
              parallelParam = '-T 2C'
            }
            def skipTestMvnParam = '-DskipTests' // tests will be run on later stages in parallel
            def customSettings = '--settings /tmp/settings.xml'
            def profileArg = "-P ${params.profile}"
            sh "mvn clean install -Dmaven.repo.local=.m2 --no-transfer-progress $profileArg $parallelParam $skipTestMvnParam $customSettings"
            archiveArtifacts "**/target/*.jar"
          } // script
        }
      } // steps
    } // stage
    stage("Verification") {
      parallel {
        stage('Integration Tests') {
          agent { label 'master' }
          steps {
            script {
              withDockerContainer(
                  image: 'maven:3-jdk-8',
                  args: '--net="host" -e MAVEN_REMOTE_REPOSITORY='+ params.mavenRepository,
                  toolName: env.DOCKER_TOOL_NAME) {
                sh 'curl -o /tmp/settings.xml https://raw.githubusercontent.com/amirmv2006/build-jenk/sandbox/generic-maven-settings.xml'
                def customSettings = '--settings /tmp/settings.xml'
                def profileArg = "-P ${params.profile}"
                def skipTestMvnParam = '-DskipUnitTests'
                try {
                  // You can override the credential to be used
                  sh "mvn verify -T 2C --no-transfer-progress $skipTestMvnParam $profileArg $customSettings"
                } finally {
                  junit '**/target/failsafe-reports/*.xml'
                }
              }
            }
          }
        }
        stage('Run Unit Tests and Sonar') {
          agent { label 'master' }
          steps {
            withDockerContainer(
                image: 'maven:3-jdk-8',
                args: '--net="host" -e MAVEN_REMOTE_REPOSITORY='+ params.mavenRepository +
                    ' -e SONAR_HOST_URL="' + sonarUrl + '" -e SONAR_TOKEN="'+ sonarToken + '" ',
                toolName: env.DOCKER_TOOL_NAME
            ) {
              script {
                sh 'curl -o /tmp/settings.xml https://raw.githubusercontent.com/amirmv2006/build-jenk/sandbox/generic-maven-settings.xml'
                def customSettings = '--settings /tmp/settings.xml'
                def profileArg = "-P ${params.profile}"
                def skipTestMvnParam = '-DskipIntegrationTests'
                try{
                  withSonarQubeEnv(credentialsId: 'sonar-token', installationName: 'sonar') {
                    // You can override the credential to be used
                    def sonarGoal = "sonar:sonar"
                    // unit tests should always be runnable in parallel, unless someone broke the law!
                    def parallelParam = '-T 2C'
                    // jacoco check is done on verify phase
                    sh "mvn verify $sonarGoal --no-transfer-progress $skipTestMvnParam $parallelParam $profileArg $customSettings"
                  }
                } finally {
                  junit '**/target/surefire-reports/*.xml'
                }
              }
              timeout(1) {
                waitUntil {
                  script {
                    fileExists('target/sonar/report-task.txt')
                  }
                }
                script {
                  def status = 'PENDING'
                  waitUntil {
                    def properties = readProperties file:'target/sonar/report-task.txt'
                    def task_response = httpRequest properties.ceTaskUrl
                    echo "Sonar Task JSON: ${task_response.content}"
                    def task_data = readJSON text: task_response.content
                    echo "JSON.task: ${task_data.task}"
                    echo "JSON.task: ${task_data.task.status}"
                    status = task_data.task.status
                    status != "PENDING" && status != "IN_PROGRESS"
                  }
                  def properties = readProperties file:'target/sonar/report-task.txt'
                  def analyses_response = httpRequest properties.ceTaskUrl.replaceAll("/api/ce/task", "/api/project_analyses/search?project=ir.amv.snippets%3Areference-initial-setup&")
                  def analysesJson = readJSON text: analyses_response.content
                  def analyses = analysesJson.analyses
                  echo "$analyses"
                  def found = false;
                  for (def analize : analyses) {
                    for (def event: analize.events) {
                      if (event.category.equals('QUALITY_GATE')) {
                        if (!found && event.name.startsWith("Red")) {
                          unstable("Sonar Quality Gate Failed: ${event.name}")
                          found = true
                        } else if (!found) {
                          found = true
                        }
                      }
                    }
                  }
                }
              } // end timeout
            } // withDockerContainer
          }
        } // end stage Run Sonar
      }
    } // stage Verification
  }
}