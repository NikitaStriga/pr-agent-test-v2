NODE_PORT = 32081
REPLICAS = 1
def cloudName (
  String DEPLOY_ENV
  ){
    switch(DEPLOY_ENV) {
      case "m4llt":
        CLOUD_NAME = "M4LLT"
        K8S_CREDENTIALS_ID="jenkins-m4llt-k8s"
        K8S_URL="https://api.m4l-k8s.komus.net:6443"
        break
      case "j7vlt":
        CLOUD_NAME = "J7VLT"
        K8S_CREDENTIALS_ID="jenkins-j7vlt-k8s"
        K8S_URL="https://api.j7v-k8s.komus.net:6443"
        break
      case "id-dev":
        CLOUD_NAME = "IMKQA-SHARED"
        K8S_CREDENTIALS_ID="jenkins-imkqa-shared-k8s"
        K8S_URL="https://192.168.93.180:6443"
        NODE_PORT = 32100
        break
      case "imkqa-shared":
        CLOUD_NAME = "IMKQA-SHARED"
        K8S_CREDENTIALS_ID="jenkins-imkqa-shared-k8s"
        K8S_URL="https://192.168.93.180:6443"
        NODE_PORT = 32101
        REPLICAS = 2
        break
      case "lt":
        CLOUD_NAME = "LT"
        K8S_CREDENTIALS_ID="jenkins-lt-k8s"
        K8S_URL="https://192.168.56.102:6443"
        break
      case "r291lt":
        CLOUD_NAME = "R291LT"
        K8S_CREDENTIALS_ID="jenkins-e3y-lt-k8s"
        K8S_URL="https://api.e3y-lt-k8s.komus.net:6443"
        break
      case "z987-lt":
        CLOUD_NAME = "Z987-LT"
        K8S_CREDENTIALS_ID="jenkins-z987-lt-k8s"
        K8S_URL="https://api.z987-lt-k8s.komus.net:6443"
        break
      case "prod":
        CLOUD_NAME = "IMK-PROD"
        K8S_CREDENTIALS_ID="jenkins-imk-prod-k8s"
        K8S_URL="https://api.imk-p-k8s.komus.net:6443"
        REPLICAS = 2
        break
      case "prod-r3":
        CLOUD_NAME = "IMK-PROD-R3"
        K8S_CREDENTIALS_ID="jenkins-imkt-prod-r3-k8s"
        K8S_URL="https://api.p-k8s.komus.net:6443"
        REPLICAS = 2
        break
    }
  }

pipeline {
  agent {
    node {
      label 'ci1s'
    }
  }

  options {
    timestamps ()
    ansiColor('xterm')
    buildDiscarder(logRotator(daysToKeepStr: '21'))
    disableConcurrentBuilds()
  }

  parameters {
    string(
      name: 'BRANCH_NAME',
      defaultValue: "master",
      description: 'Git tag/branch to find correct artifact for prod env this values will be get on the vault',
    )
    choice(name: 'DEPLOY_ENV',
      choices: ['id-dev','imkqa-shared','m4llt', 'j7vlt', 'lt','r291lt','z987-lt','prod','prod-r3'],
      description: 'Environment selector',
    )
    string(
      name: 'DOCKERFILE_NAME',
      defaultValue: "Dockerfile",
      description: 'Set Dockerfile name',
    )
    booleanParam(
        name: 'BUILD',
        defaultValue: true,
        description: 'Build app or no'
    )
  }
  environment{
    GIT_CREDENTIALS_ID="jenkins-bb-user"
    PROJECT_TO_BUILD_REPO="ssh://git@bitbucket-app.komus.net:7999/gen/idgenerator.git"
    KOMUS_REGISTRY="registry.komus.net"
    KOMUS_REGISTRY_CRED_ID="komus-docker-rgistry"
    BUILD_DIR="idgenerator_build"
    VAULT_SECRET_PATH="imk/idgenerator/idapp-${DEPLOY_ENV}"
    VAULT_CERT_PATH="cert/komus.net/cert-base64"
    VAULT_CREDENTIAL_ID="vault-dbt-projects-ci1m-reader"
    VAULT_URL="https://deploy.komus.net:8201"
    HELM_REPO_URL=".ci/helm"
    HELM_VALUES_PATH=".ci/helm/values.yaml"
    INGRESS_HOSTNAME="idgenerator-${DEPLOY_ENV}.komus.net"
  }

  stages {
    stage('Clone IDGENERATOR source code') {
      steps {
        dir(BUILD_DIR){
          checkout([
            $class: "GitSCM",
            branches: [[name: BRANCH_NAME]],
            doGenerateSubmoduleConfigurations: false,
            extensions: [[$class: "CloneOption",
                          noTags: false,
                          shallow: false]],
            submoduleCfg: [],
            userRemoteConfigs: [[credentialsId: GIT_CREDENTIALS_ID,
                                  url: PROJECT_TO_BUILD_REPO]]
          ])
        }
      }
    }

    stage("Prepare vars") {
      steps {
        dir(BUILD_DIR){
          script {
            println "prepare vars"
            vaultConfiguration = [vaultUrl: VAULT_URL,  vaultCredentialId: VAULT_CREDENTIAL_ID, engineVersion: 2]
            secretsFromVault = [
                [path: VAULT_SECRET_PATH, engineVersion: 2, secretValues: [
                [envVar: 'DB_USERNAME', vaultKey: 'DB_USERNAME'],
                [envVar: 'DB_PASSWORD', vaultKey: 'DB_PASSWORD'],
                [envVar: 'SPRING_USER', vaultKey: 'SPRING_USER'],
                [envVar: 'SPRING_PASSWORD', vaultKey: 'SPRING_PASSWORD'],
                [envVar: 'SPRING_ADMIN', vaultKey: 'SPRING_ADMIN'],
                [envVar: 'SPRING_ADMIN_PASSWORD', vaultKey: 'SPRING_ADMIN_PASSWORD'],
                [envVar: 'DB_URL', vaultKey: 'DB_URL']
                ]]
            ]
            certsFromVault = [
                [path: VAULT_CERT_PATH, engineVersion: 2, secretValues: [
                [envVar: 'KOMUS_CERT', vaultKey: 'crt'],
                [envVar: 'KOMUS_KEY', vaultKey: 'key']
                ]]
            ]
            APP_VERSION = readMavenPom().getVersion() as String
            APP_NAME = 	readMavenPom().getArtifactId() as String
            BUILD_ARTIFACT="${APP_NAME}_${APP_VERSION}"
            BUILD_ARTIFACT_ID="${APP_NAME}_${APP_VERSION}_${params.DEPLOY_ENV}"
            DEPLOY_IMAGE="${KOMUS_REGISTRY}/${APP_NAME}:${APP_VERSION}-${DEPLOY_ENV}"

            currentBuild.displayName = "#${BUILD_NUMBER} - ${DEPLOY_ENV} - ${BRANCH_NAME}"
          }
        }
      }
    }

    stage('Build artifact') {
      agent {
        docker { 
          image 'docker.komus.net/maven:3.8.6-openjdk-18' 
          reuseNode true
        }
      }
      when {
        environment name: 'BUILD' , value: 'true'
      }
      steps{
        dir(BUILD_DIR){
          script {
            sh '''
              unset MAVEN_CONFIG
              mvn -version
              bash ./mvnw -B -DskipTests clean package
            '''
          }
        }
      }
    }

    stage('Build and push Docker Image') {
      when {
        environment name: 'BUILD', value: 'true'
      }
      steps {
        dir(BUILD_DIR){
          script {
            docker.withRegistry("https://${KOMUS_REGISTRY}", "${KOMUS_REGISTRY_CRED_ID}") {
              def idgenerator_image = docker.build(
                "${APP_NAME}", "--network host -f ${DOCKERFILE_NAME} .")

              idgenerator_image.push("${APP_VERSION}-${env.BUILD_NUMBER}-${DEPLOY_ENV}")
              idgenerator_image.push("${APP_VERSION}-${DEPLOY_ENV}")
              DEPLOY_IMAGE="${KOMUS_REGISTRY}/${APP_NAME}:${APP_VERSION}-${env.BUILD_NUMBER}-${DEPLOY_ENV}"
              sh "docker images -q ${idgenerator_image.id} | xargs docker rmi -f"
            }
          }
        }
      }
    }

    stage('Deploy idegenerator in k8s') {
      steps {
        cloudName(DEPLOY_ENV)
        withKubeConfig([credentialsId: K8S_CREDENTIALS_ID, serverUrl: K8S_URL]) {
          withVault([configuration: vaultConfiguration, vaultSecrets: secretsFromVault]){
            script {
              env.HELM_DEPLOY_ARGS = "--install ${APP_NAME}-${DEPLOY_ENV} "
              env.HELM_DEPLOY_ARGS += " --set environment=${DEPLOY_ENV}"
              env.HELM_DEPLOY_ARGS += " --set idgenerator.image=${DEPLOY_IMAGE}"
              env.HELM_DEPLOY_ARGS += " --set idgenerator.service.nodePort=${NODE_PORT}"
              env.HELM_DEPLOY_ARGS += " --set idgenerator.replicas=${REPLICAS}"
              env.HELM_DEPLOY_ARGS += " --set idgenerator.environments.SPRING_DATASOURCE_USERNAME=${DB_USERNAME}"
              env.HELM_DEPLOY_ARGS += " --set idgenerator.environments.SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}"
              env.HELM_DEPLOY_ARGS += " --set idgenerator.environments.'SPRING\\.SECURITY\\.USER\\.NAME'=${SPRING_USER}"
              env.HELM_DEPLOY_ARGS += " --set idgenerator.environments.'SPRING\\.SECURITY\\.USER\\.PASSWORD'=${SPRING_PASSWORD}"
              env.HELM_DEPLOY_ARGS += " --set idgenerator.environments.'SPRING\\.SECURITY\\.ADMIN\\.NAME'=${SPRING_ADMIN}"
              env.HELM_DEPLOY_ARGS += " --set idgenerator.environments.'SPRING\\.SECURITY\\.ADMIN\\.PASSWORD'=${SPRING_ADMIN_PASSWORD}"
              env.HELM_DEPLOY_ARGS += " --set idgenerator.environments.SPRING_DATASOURCE_URL=${DB_URL}"
              env.HELM_DEPLOY_ARGS += " --set ingress.hostname=${INGRESS_HOSTNAME}"
              env.HELM_DEPLOY_ARGS += " --set build_no=${BUILD_NUMBER}"
              env.HELM_DEPLOY_ARGS += " --set branch_name=${BRANCH_NAME}"
              env.HELM_DEPLOY_ARGS += " --set createNameSpace=false"
              env.APP_NAME="${APP_NAME}"
            }
            sh '''
              echo "###DEPLOY IN K8S###"
              helm upgrade --cleanup-on-fail ${HELM_DEPLOY_ARGS} --namespace ${APP_NAME}-${DEPLOY_ENV} --values ${HELM_VALUES_PATH} ${HELM_REPO_URL} --create-namespace --wait
            '''
          }
        }
      }
    }

    stage('Print info message') {
      steps {
        script{
          println("Ingress hostname: http://${INGRESS_HOSTNAME}")
          println("Docker image: ${DEPLOY_IMAGE}")
        }
      }
    }
  }

  post { 
    always { 
      deleteDir()
    }
  }
}
