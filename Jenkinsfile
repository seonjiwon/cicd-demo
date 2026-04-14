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
        // 1단계: Gradle 빌드 (테스트 제외)
        stage('1. Build') {
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

        // 3단계: Docker 이미지 빌드 및 Docker Hub에 Push
        stage('3. Docker Build & Push') {
            steps {
                script {
                    docker.withRegistry('https://index.docker.io/v1/', 'docker-hub-key') {
                        // 빌드 번호 태그 + latest 태그로 push
                        def appImage = docker.build("${REPOSITORY}:${BUILD_NUMBER}")
                        appImage.push()
                        appImage.push('latest')
                    }
                }
            }
        }

        // 4단계: Blue/Green 배포
        stage('4. Deploy (Blue/Green)') {
            steps {
                // 비밀번호 기반 SSH 접속
                withCredentials([usernamePassword(
                        credentialsId: 'deploy-server-ssh-key',
                        usernameVariable: 'SSH_USER',
                        passwordVariable: 'SSH_PASS'
                )]) {
                    sh """
                        sshpass -p \$SSH_PASS ssh -o StrictHostKeyChecking=no \$SSH_USER@${DEPLOY_HOST} '

                            # ===== 1. 현재 활성 환경 확인 =====
                            # Nginx 설정 파일에서 현재 upstream이 blue인지 green인지 파악
                            CURRENT=\$(grep -o "sw_team_2_[a-z]*" ${NGINX_CONF_PATH} | head -1)

                            # 현재가 blue면 green에 배포, green이면 blue에 배포
                            if [ "\$CURRENT" = "sw_team_2_blue" ]; then
                                TARGET="sw_team_2_green"
                                TARGET_PORT=8103
                            else
                                TARGET="sw_team_2_blue"
                                TARGET_PORT=8102
                            fi

                            # ===== 2. 새 이미지 Pull =====
                            docker pull ${REPOSITORY}:${BUILD_NUMBER}

                            # ===== 3. 비활성 컨테이너 정리 후 새 버전 실행 =====
                            docker stop \$TARGET || true
                            docker rm \$TARGET || true

                            docker run -d --name \$TARGET \
                                --network sw_team_2_network \
                                -p \$TARGET_PORT:8080 \
                                -e DB_HOST=${DB_HOST} \
                                -e DB_NAME=${DB_NAME} \
                                -e DB_USERNAME=${DB_USER} \
                                -e DB_PASSWORD=${DB_PASS} \
                                ${REPOSITORY}:${BUILD_NUMBER}

                            # ===== 4. Health Check =====
                            # 3초 간격, 최대 10회 시도 (총 30초 대기)
                            for i in \$(seq 1 10); do
                                if curl -sf http://localhost:\$TARGET_PORT/actuator/health > /dev/null 2>&1; then
                                    echo "Health check passed"
                                    break
                                fi
                                if [ \$i -eq 10 ]; then
                                    echo "Health check failed"
                                    docker stop \$TARGET || true
                                    docker rm \$TARGET || true
                                    exit 1
                                fi
                                sleep 3
                            done

                            # ===== 5. Nginx 트래픽 전환 =====
                            sed -i "s/\$CURRENT/\$TARGET/" ${NGINX_CONF_PATH}
                            docker exec sw_team_2_nginx nginx -s reload

                            # ===== 6. 이전 컨테이너 정리 =====
                            docker stop \$CURRENT || true
                            docker rm \$CURRENT || true

                            echo "Deployed \$TARGET successfully"
                        '
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