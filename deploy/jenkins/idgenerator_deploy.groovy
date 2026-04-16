VAULT_CREDS = 'env'
def vaultConfiguration = [vaultUrl: 'https://deploy.komus.net:8201',  vaultCredentialId: 'env', engineVersion: 2]
def secretsFromVault = [
    [path: '${KOMUS_VAULT_SECRET_ROOT}', engineVersion: 2, secretValues: [
    [envVar: 'PG_USERNAME', vaultKey: 'PG_USERNAME'],
    [envVar: 'PG_PASSWORD', vaultKey: 'PG_PASSWORD'],
    [envVar: 'SPRING_USER', vaultKey: 'SPRING_USER'],
    [envVar: 'SPRING_PASSWORD', vaultKey: 'SPRING_PASSWORD'],
    [envVar: 'DB_NAME', vaultKey: 'DB_NAME']
    ]]
]
GIT_URL = 'ssh://bitbucket-app.komus.net/gen/idgenerator.git'
GIT_CREDENTIALS = '083203cf-0a7a-4f5b-84f3-eeb7ba1971a4'
VAULT_URL = 'https://deploy.komus.net:8201'
//ANSIBLE_CREDENTIALS_ID = '954d05a2-19d6-40be-a0b5-674d98b36dfa'
ANSIBLE_CREDENTIALS_ID =  '2ui3d939-ka39-aks7-lo4un-2938hdaw2djkd'
pipeline {

   options {
      timeout(time: 15, unit: 'MINUTES')
    }

    agent {
      node label: 'master'
    }

    parameters {
      string(name: 'GIT_BRANCH',
            defaultValue: 'CHANGEME',
            description: 'Git tag/branch to find correct artifact for prod env this values will be get on the vault'
            )
      choice(name: 'ENVIRONMENT', choices: ['lt', 'dev','qa','prod'], description: 'Choose environment')
      choice(name: 'BUILD', choices: ['yes','no'], description: 'Build app or no?')
    }
    environment {
      NEXUS_CREDENTIAL_ID = "nexus-user"
      REGISTRY_URL = "registry.komus.net"
      NEXUS_VERSION = "nexus3"
      NEXUS_PROTOCOL = "https"
      NEXUS_URL = "nexus.komus.net"
      KOMUS_VAULT_SECRET_ROOT = "env/idapp-${params.ENVIRONMENT}"

      NEXUS_REPOSITORY = "idgenerator_app"
      pathToComposeTemplate = "./deploy/template/docker-compose.yml"
      pathToTargetComposeFile = "./docker-compose-${params.ENVIRONMENT}.yml"


   }
    stages {
      stage('Prepare vars') {
        steps {
          script {
              println "prepare vars"
              /* this map will applying for generating docker compose file
              these vars are sets in the Jenkins params, otherwise this parameters are empty for the dev enviroment - the default value will be set
              if env = PROD these vars will be specifed in Vault
              PG_USERNAME
              PG_PASSWORD
              SPRING_USER
              SPRING_PASSWORD
              */
              APP_VERSION = readMavenPom().getVersion() as String
              APP_NAME = 	readMavenPom().getArtifactId() as String
              COMPOSE_ARTIFACT_ID = "${APP_NAME}_${params.ENVIRONMENT}"
              //COMPOSE_ARTIFACT_ID = "${APP_NAME}-${params.ENVIRONMENT}-${APP_VERSION}"
              switch(ENVIRONMENT) {
                  case lt:
                      DB_HOST = "lt-repapp1.komus.net"
                      break;
                  case dev:
                      DB_HOST = "r183-iddb1dev.komus.net"
                      break;
                  case qa:
                      DB_HOST = "r183-iddb1qa.komus.net"
                      break;
                  case prod:
                      DB_HOST = "r183-unknown.komus.net"
                      break;
              }
              //DB_HOST = "r183-iddb1${params.ENVIRONMENT}.komus.net"
              ARTIFACT_FILE_TYPE="tar.gz"
              //UPLOAD_ARTIFACT = "${APP_NAME}_${APP_VERSION}_${params.ENVIRONMENT}.tar.gz"
              GROUP_ID = "applications"
              // Application with SNAPSHOT version will be deploy only dev and test env.
              if (!APP_VERSION.endsWith("-SNAPSHOT")) {
                 withVault([configuration: vaultConfiguration, vaultSecrets: secretsFromVault]) {
                 PG_USERNAME = "${env.PG_USERNAME}"
                 PG_PASSWORD = "${env.PG_PASSWORD}"
                 SPRING_USER = "${env.SPRING_USER}"
                 SPRING_PASSWORD = "${env.SPRING_PASSWORD}"
                 //def arguments
                  }
                 }
                 else
                 {
                 ENVIRONMENT = "${params.ENVIRONMENT}"
                 withVault([configuration: vaultConfiguration, vaultSecrets: secretsFromVault]){
                 PG_USERNAME = "${env.PG_USERNAME}"
                 PG_PASSWORD = "${env.PG_PASSWORD}"
                 SPRING_USER = "${env.SPRING_USER}"
                 SPRING_PASSWORD = "${env.SPRING_PASSWORD}"
                 DB_NAME = "${env.DB_NAME}"
                 }

              }
              BUILD_ARTIFACT="${APP_NAME}_${APP_VERSION}"
              BUILD_ARTIFACT_ID="${APP_NAME}_${APP_VERSION}_${params.ENVIRONMENT}"
              CURRENT_IMAGE = "${REGISTRY_URL}/${APP_NAME}/${APP_NAME}:${APP_VERSION}-${env.BUILD_NUMBER}"
              //CURRENT_IMAGE = "${APP_NAME}:${APP_VERSION}"
              COMPOSE_VARS = ["APP_NAME":"${APP_NAME}","APP_VERSION":"${APP_VERSION}","IMAGE":"${CURRENT_IMAGE}",
                                "PG_USERNAME":"${PG_USERNAME}","PG_PASSWORD":"${PG_PASSWORD}","SPRING_USER":"${SPRING_USER}",
                                "SPRING_PASSWORD":"${SPRING_PASSWORD}","DB_NAME":"${DB_NAME}","DB_HOST":"${DB_HOST}"]

              currentBuild.displayName = "# ${APP_NAME}${APP_VERSION} Build number ${env.BUILD_NUMBER}"
          }
          }
          }

      stage('Checkout') {
        steps {
          checkout([$class: 'GitSCM',
                   branches: [[name: params.GIT_BRANCH]],
                   userRemoteConfigs: [[url: "${GIT_URL}",
                   redentialsId: "${GIT_CREDENTIALS}"]]
                  ])
        }
      }
        stage('Generate Compose') {
        steps{
            script {
                UPLOAD_ARTIFACT = "${APP_NAME}_${BUILD_NUMBER}_${params.ENVIRONMENT}.tar.gz"
                def resultMap = [:]
                def composeTemplate = readFile pathToComposeTemplate
                resultMap.putAll(COMPOSE_VARS)
                def targetComposeFile = readFile pathToTargetComposeFile
                def compose = dotemplate(composeTemplate, resultMap)
                writeFile(file: pathToTargetComposeFile, text: compose)
                println UPLOAD_ARTIFACT
                sh """touch ${UPLOAD_ARTIFACT}
                tar -czf ${UPLOAD_ARTIFACT} docker-compose-${params.ENVIRONMENT}.yml
                cp docker-compose-${params.ENVIRONMENT}.yml deploy/ansible
                """


            }
        }
      }
      stage('Upload artifact to Nexus') {
        steps{
            nexusArtifactUploader(
              nexusVersion: NEXUS_VERSION,
              protocol: NEXUS_PROTOCOL,
              nexusUrl: NEXUS_URL,
              groupId:  GROUP_ID,
              version:  BUILD_NUMBER,
              repository: NEXUS_REPOSITORY,
              credentialsId: NEXUS_CREDENTIAL_ID,
              artifacts: [
                  // Artifact generated such as .jar, .ear and .war files.
                  [artifactId: COMPOSE_ARTIFACT_ID,
                  classifier: '',
                  file: "${UPLOAD_ARTIFACT}",
                  type: ARTIFACT_FILE_TYPE]
              ]
            );
          }
        }
      stage('Build artifact') {
        steps{
          script {
          if ( params.BUILD == "yes") {
          sh """export JAVA_HOME=/usr/lib/jvm/java-11-openjdk
          mvn -version
          bash ./mvnw -B -DskipTests clean package"""
          } else {
            println "skip build app with maven"
          }
        }
        }
      }
      stage('Build Docker Image') {
        steps {
          script {
          withDockerRegistry(credentialsId: NEXUS_CREDENTIAL_ID, url: "https://${REGISTRY_URL}") {
             if (params.BUILD == "yes") {
             sh "docker build -t ${REGISTRY_URL}/${APP_NAME}/${APP_NAME}:${APP_VERSION}-${env.BUILD_NUMBER} ."
             sh "docker push ${REGISTRY_URL}/${APP_NAME}/${APP_NAME}:${APP_VERSION}-${env.BUILD_NUMBER}"
             sh "docker tag ${REGISTRY_URL}/${APP_NAME}/${APP_NAME}:${APP_VERSION}-${env.BUILD_NUMBER}  ${REGISTRY_URL}/${APP_NAME}/${APP_NAME}:${APP_VERSION}"
             sh "docker push ${REGISTRY_URL}/${APP_NAME}/${APP_NAME}:${APP_VERSION}"
             sh "docker rmi -f ${REGISTRY_URL}/${APP_NAME}/${APP_NAME}:${APP_VERSION}"
             sh "docker logout ${REGISTRY_URL}"

             milestone(1)
             } else {
              println  "skip building docker images"
             }
          }
          }
       }
      }
      stage('Deploy idgenerator') {
        steps{
          dir("deploy/ansible"){
             withCredentials([[
                $class       : 'VaultTokenCredentialBinding',
                credentialsId: 'env', //token?
                vaultAddr    : VAULT_URL,
                tokenVariable: 'VAULT_TOKEN',
                ]]) {
                   script {
                      ansibleExtras = "-e build_env=${params.ENVIRONMENT}"
                      ansibleExtras += " -e komus_vault_url=${VAULT_URL}"
                      ansibleExtras += " -e idgenerator_vault_token=${VAULT_TOKEN}"
                      ansibleExtras += " -e idgenerator_secret_vault_root=${KOMUS_VAULT_SECRET_ROOT}"
                      ansibleExtras += " -e app_version=${APP_VERSION}"
                      ansibleExtras += " -e image_deploy=${CURRENT_IMAGE}"
                      ansibleExtras += " -e app_name=${APP_NAME}"
                      ansibleExtras += " -e build_number=${env.BUILD_NUMBER}"
                     // ansibleExtras += " -vv"
                      ansiblePlaybook(
                         credentialsId: "${ANSIBLE_CREDENTIALS_ID}",
                         inventory: "enviroments/host.ini",
                         playbook: "playbooks/deploy_idgenerator.yml",
                         forks: 5,
                         extras: ansibleExtras,
                         colorized: true
                      )
                   }
                 }
           }
         }
       }
      stage('Clear workspace') {
        steps{
           deleteDir()
        }
      }
//Close pipeline
  }
}


@NonCPS
String dotemplate(text, bind) {
    def engine = new groovy.text.GStringTemplateEngine()
    def template = engine.createTemplate(text).make(bind)
    return template.toString()
}
