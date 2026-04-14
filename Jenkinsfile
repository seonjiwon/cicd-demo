pipeline {
    agent any

    environment {
        // 1. 유진님의 도커 허브 및 배포 서버 정보
        DOCKERHUB_ID = "yujinjo1"
        IMAGE_NAME = "cicd-demo-app"
        REPOSITORY = "${DOCKERHUB_ID}/${IMAGE_NAME}"
        DEPLOY_SERVER = "sw_team_2@172.21.33.69"

        // 2. 젠킨스 Credentials(Secret text)에서 유진님이 설정한 DB 정보 가져오기
        DB_HOST = credentials('db-host')     // sw_team_2_mysql
        DB_NAME = credentials('db-name')     // todo
        DB_PASS = credentials('db-password') // 1234
        DB_USER = "root"                     // 기본값 root 사용
    }

    stages {
        stage('1. Checkout') {
            steps {
                // 팀원 레포지토리에서 최신 코드를 가져옵니다.
                git branch: 'main', url: 'https://github.com/seonjiwon/cicd-demo.git'
            }
        }

        stage('2. Build') {
            steps {
                sh 'chmod +x ./gradlew'
                // 빌드 속도를 위해 테스트는 제외하고 빌드합니다.
                sh './gradlew clean build -x test'
            }
        }

        stage('3. SonarQube Analysis') {
            steps {
                script {
                    // 유진님이 도구 설정에서 만든 이름 'SonarScanner'와 'SonarQube'
                    def scannerHome = tool 'SonarScanner'
                    withSonarQubeEnv('SonarQube') {
                        sh "${scannerHome}/bin/sonar-scanner"
                    }
                }
            }
        }

        stage('4. Docker Build & Push') {
            steps {
                script {
                    // 유진님이 만든 도커 허브 열쇠 ID 'docker-hub-key'
                    docker.withRegistry('https://index.docker.io/v1/', 'docker-hub-key') {
                        // Dockerfile을 읽어 이미지를 빌드하고 도커 허브로 전송합니다.
                        def appImage = docker.build("${REPOSITORY}:latest")
                        appImage.push()
                    }
                }
            }
        }

        stage('5. Remote Deploy (SSH)') {
            steps {
                // 유진님이 만든 배포 서버 열쇠 ID 'deploy-server-ssh-key'
                sshagent(credentials: ['deploy-server-ssh-key']) {
                    sh """
                        ssh -o StrictHostKeyChecking=no ${DEPLOY_SERVER} "
                            # 1. 도커 허브에서 최신 이미지 내려받기
                            docker pull ${REPOSITORY}:latest
                            
                            # 2. 기존 실행 중인 앱 컨테이너 중지 및 삭제 (실패해도 무시)
                            docker stop my-app || true
                            docker rm my-app || true
                            
                            # 3. 새 컨테이너 실행 (이미 떠 있는 MySQL 컨테이너와 연결!)
                            # -e 옵션을 통해 application.yml의 환경변수에 값을 주입합니다.
                            docker run -d --name my-app \
                              -p 8080:8080 \
                              -e DB_HOST=${DB_HOST} \
                              -e DB_NAME=${DB_NAME} \
                              -e DB_USERNAME=${DB_USER} \
                              -e DB_PASSWORD=${DB_PASS} \
                              ${REPOSITORY}:latest
                        "
                    """
                }
            }
        }
    }

    post {
        success {
            echo '유진 지휘자님, 모든 과정이 성공적으로 끝났습니다! 🚀'
        }
        failure {
            echo '문제가 발생했습니다. 젠킨스 콘솔 로그를 확인해 주세요! 🔴'
        }
    }
}