pipeline {
    agent any

    environment {
        // Docker Hub 계정 및 이미지 정보
        DOCKERHUB_ID = "yujinjo1"
        IMAGE_NAME = "cicd-demo-app"
        REPOSITORY = "${DOCKERHUB_ID}/${IMAGE_NAME}"

        // 배포 서버 IP (SSH 접속용)
        DEPLOY_HOST = "172.21.33.69"

        // Jenkins Credentials에서 DB 접속 정보 가져오기
        DB_HOST = credentials('DB_HOST')
        DB_NAME = credentials('DB_NAME')
        DB_PASS = credentials('DB_PASSWORD')
        DB_USER = "root"

        // 배포 서버의 Nginx 설정 파일 경로
        NGINX_CONF_PATH = "/home/sw_team_2/app/nginx/conf.d/app.conf"
    }

    stages {
        // 1단계: Gradle 빌드 + 테스트 + Jacoco 커버리지 리포트 생성
        stage('1. Build & Test') {
            steps {
                sh 'chmod +x ./gradlew'
                sh './gradlew clean build'
            }
        }

        // 2단계: SonarQube 코드 품질 분석
        stage('2. SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh './gradlew sonar'
                }
            }
        }

        // 3단계: Quality Gate 결과 확인
        // SonarQube가 분석 완료 후 통과/실패 판정을 내릴 때까지 대기
        // 실패 시 파이프라인 중단
        stage('3. Quality Gate') {
            steps {
                timeout(time: 1, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        // 4단계: Docker 이미지 빌드 및 Docker Hub에 Push
        stage('4. Docker Build & Push') {
            steps {
                script {
                    docker.withRegistry('https://index.docker.io/v1/', 'docker-hub-key') {
                        def appImage = docker.build("${REPOSITORY}:latest")
                        appImage.push()
                    }
                }
            }
        }

        // 5단계: Jenkins 서버의 Docker 이미지 정리
        // push 완료 후 로컬 이미지를 삭제하여 디스크 공간 확보
        stage('5. Cleanup Jenkins Image') {
            steps {
                sh "docker rmi ${REPOSITORY}:latest || true"
            }
        }

        // 6단계: Blue/Green 배포
        stage('6. Deploy (Blue/Green)') {
            steps {
                withCredentials([usernamePassword(
                        credentialsId: 'deploy-server-ssh-key',
                        usernameVariable: 'SSH_USER',
                        passwordVariable: 'SSH_PASS'
                )]) {
                    sh """
                sshpass -p \$SSH_PASS scp -o StrictHostKeyChecking=no scripts/deploy.sh \$SSH_USER@${DEPLOY_HOST}:/tmp/deploy.sh
                sshpass -p \$SSH_PASS ssh -o StrictHostKeyChecking=no \$SSH_USER@${DEPLOY_HOST} 'bash /tmp/deploy.sh ${REPOSITORY} ${DB_HOST} ${DB_NAME} ${DB_USER} ${DB_PASS} ${NGINX_CONF_PATH}'
            """
                }
            }
        }
    }

    post {
        success {
            echo '배포 성공!'
        }
        failure {
            echo '배포 실패. 콘솔 로그를 확인해주세요!'
        }
    }
}