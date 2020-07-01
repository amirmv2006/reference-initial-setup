# Setup Local Jenkins
You need Docker on your machine. Then we can set up Jenkins and Sonar to build this project.

## Run Sonar 
Use this command to run Sonar
```shell script
docker run -d --restart unless-stopped --name amir-sonar -p 9000:9000 -v sonar-conf:/opt/sonarqube/conf -v sonar-data:/opt/sonarqube/data -v sonar-logs:/opt/sonarqube/logs -v sonar-extensions:/opt/sonarqube/extensions sonarqube
```
In order for Jenkins to be able to communicate with Sonar, we need to create an authentication token. To do this, login
to the [Sonar](http://localhost:9000/account/security/). The default username/password for Sonar is admin/admin. Under
"Generate Tokens" enter name "Jenkins" and select "Generate". Select copy and keep this token somewhere, we will need
 it on the Jenkins config.
![SonarGenerateToken](SonarTokenGenerate.png "Sonar Generate Token")

## Run Jenkins
Use this command to run Jenkins
```shell script
docker run -d --restart unless-stopped --name amir-jenkins -p 9090:8080 -u root -v /var/run/docker.sock:/var/run/docker.sock -v amir-jenkins:/var/jenkins_home amirmv2006/amir-jenkins
```
