
def call(Map options = [:]) {

  
  node('BUILD') {

    def releaseVersion = ''
    def channel = options.slackChannel //'#mcp-brazil-ci'
    def image = options.image // 'maven:3.5-jdk-8' {"image": "'maven:3.5-jdk-8", "channel": "#mcp-brazil-ci"}
    def PORT = options.port
    def PATH = options.path
    def deployArtifact = options.deployArtifact

    try{

      stage('Checkout Source') {
        checkout(scm)
      }
    
      withDockerContainer(args: '--privileged -u 0 -v /data/workspace/.m2/:/root/.m2/' , image: image) {
    
        stage('Adjust Version') {
          sh("/bin/sh ./change-pom-version.sh ${env.BUILD_ID}")
          releaseVersion = readFile('RELEASE_VERSION').trim()
        }
    
        if (isMasterBranch() || isJava8Branch()) {
          stage('Build/Publish Artifact') {
            if(deployArtifact) {
              sh('mvn deploy -DskipTests -DAWS_DEFAULT_REGION=sa-east-1')
            } else {
              sh('mvn install -DskipTests -DAWS_DEFAULT_REGION=sa-east-1')
            }
          }
          
          APPNAME = readMavenPom().getArtifactId()
          VERSION = readMavenPom().getVersion()
          ARTIFACT = "${APPNAME}-${VERSION}"

          stage('Adjust CodeDeploy Scripts') {
            sh("grep -rl __APPNAME__ codedeploy/* | xargs sed -i 's/__APPNAME__/${APPNAME}/g'")
            sh("grep -rl __ARTIFACT__ codedeploy/* | xargs sed -i 's/__ARTIFACT__/${ARTIFACT}/g'")
            sh("grep -rl __PORT__ codedeploy/* | xargs sed -i 's/__PORT__/${PORT}/g'")
            sh("grep -rl __PATH__ codedeploy/* | xargs sed -i 's/__PATH__/${PATH}/g'")
            sh("grep -rl __VERSION__ codedeploy/* | xargs sed -i 's/__VERSION__/${VERSION}/g'")
          }

          stage('S3 CodeDeploy Artifact') {
            sh("set +e ; tar -czvf ${ARTIFACT}.tgz target/*.tar.gz  -C codedeploy/ ./ ; set -e")
            withAWS(credentials:'7742c56f-56a1-4596-a39f-458c44158474', region:'sa-east-1') {
              s3Upload(file:"${ARTIFACT}.tgz", bucket:'spo-mcp-deploy', path:"${APPNAME}/", contentEncoding: 'gzip')  
            }
          }

          stage('Clean Repo changes'){
            sh("git checkout -- .")
            sh("git submodule foreach --recursive 'git checkout -- .'")
          }
        }
      }
      
      if (isMasterBranch()) {
        stage('Tag VCS') {
          sh('git config --global user.email "project1-jenkins@test.com"')
          sh('git config --global user.name "project1 Jenkins"')        
          sh("git tag -a ${releaseVersion} -m 'Jenkins CI'")
          sshagent (credentials: ['64044bdb-d8ec-4480-940e-d29ee15ae28b']) {
            sh("git push origin ${releaseVersion}")
          }
        }
      }
    }
    catch(e) {
      currentBuild.result = "FAILED"
      throw e
    } 
    finally {
      notifyBuild(currentBuild.result, releaseVersion, channel)
      cleanWs()
    }
  }
}

def isMasterBranch() {
  "${env.BRANCH_NAME}" == 'master'
}

def isJava8Branch() {
  "${env.BRANCH_NAME}" == 'issues/17089'
}

def notifyBuild(String buildStatus, String release, String channel) {
  if (channel == null) channel = "#mcp-brazil-ci";
  buildStatus = buildStatus ?: 'SUCCESS'

  def emoji = buildStatus == 'SUCCESS' ? ':wink:' : ':sob:';
  try {
    slackSend (
      channel: channel,
      message: """${emoji} *${env.JOB_NAME}* build#${env.BUILD_NUMBER} release#${release} ${buildStatus}
      Details: ${env.BUILD_URL}""",
      rawMessage: true
    )
  } 
  catch (e) {
    echo 'Error trying to send Rocket.Chat notification: ' + e.toString()
  }
}
