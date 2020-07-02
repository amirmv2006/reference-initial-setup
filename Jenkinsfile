// A Jenkinsfile useful for Verification. Will run UnitTests+Sonar and IntegrationTests for the project
pipeline {
  agent none
  parameters {
    string(name: 'profile', defaultValue: 'Jenkins', description: 'Maven profiles to be used when running maven build')
    string(name: 'mavenRepository', defaultValue: 'https://repo1.maven.org/maven2', description: 'Remote Maven Repository')
    booleanParam(name: 'parallel', defaultValue: true, description: 'Run mvn in Parallel')
    credentials(name: 'sonarCredentials', defaultValue: 'sonar-token', credentialType: 'Secret text', description: 'Sonar Token')
  }
  stages {
    stage('Only Compile') {
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
            image: 'maven:3-jdk-11',
            args: '--net="host" --add-host=sonar:127.0.0.1 -e MAVEN_REMOTE_REPOSITORY='+ params.mavenRepository,
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
                  image: 'maven:3-jdk-11',
                  args: '--net="host" --add-host=sonar:127.0.0.1 -e MAVEN_REMOTE_REPOSITORY='+ params.mavenRepository,
                  toolName: env.DOCKER_TOOL_NAME) {
                sh 'curl -o /tmp/settings.xml https://raw.githubusercontent.com/amirmv2006/build-jenk/sandbox/generic-maven-settings.xml'
                def customSettings = '--settings /tmp/settings.xml'
                def profileArg = "-P ${params.profile}"
                def skipTestMvnParam = '-DskipUnitTests'
                try {
                  // You can override the credential to be used
                  sh "mvn verify -Dmaven.repo.local=.m2 -T 2C --no-transfer-progress $skipTestMvnParam $profileArg $customSettings"
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
                image: 'maven:3-jdk-11',
                args: '--net="host" --add-host=sonar:127.0.0.1 -e MAVEN_REMOTE_REPOSITORY='+ params.mavenRepository,
                toolName: env.DOCKER_TOOL_NAME
            ) {
              script {
                sh 'curl -o /tmp/settings.xml https://raw.githubusercontent.com/amirmv2006/build-jenk/sandbox/generic-maven-settings.xml'
                def customSettings = '--settings /tmp/settings.xml'
                def profileArg = "-P Verify,${params.profile}"
                def skipTestMvnParam = '-DskipIntegrationTests'
                try{
                  withSonarQubeEnv(credentialsId: params.sonarCredentials, installationName: 'sonar') {
                    withCredentials([string(credentialsId: params.sonarCredentials, variable: 'SONAR_TOKEN')]) {
                    }
                    // You can override the credential to be used
                    def sonarGoal = 'sonar:sonar -Dsonar.login=$SONAR_TOKEN'
                    // unit tests should always be runnable in parallel, unless someone broke the law!
                    def parallelParam = '-T 2C'
                    // jacoco check is done on verify phase
                    sh "mvn -Dmaven.repo.local=.m2 verify $sonarGoal --no-transfer-progress $skipTestMvnParam $parallelParam $profileArg $customSettings"
                  }
                  sleep(5) // copied from https://community.sonarsource.com/t/waitforqualitygate-timeout-in-jenkins/2116/9
                  timeout(time: 1, unit: 'HOURS') { // Just in case something goes wrong, pipeline will be killed after a timeout
                    def qg = waitForQualityGate() // Reuse taskId previously collected by withSonarQubeEnv
                    if (qg.status != 'OK') {
                      unstable("Sonar Quality Gate Failed: ${qg.status}")
                    }
                  }
                } finally {
                  junit '**/target/surefire-reports/*.xml'
                }
              }
            } // withDockerContainer
          }
        } // end stage Run Sonar
      }
    } // stage Verification
  }
}