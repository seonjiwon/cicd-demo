pipeline {
    agent any

    environment {
        DOCKERHUB_ID = "yujinjo1"
        IMAGE_NAME = "cicd-demo-app"
        REPOSITORY = "${DOCKERHUB_ID}/${IMAGE_NAME}"
        DEPLOY_SERVER = "sw_team_2@172.21.33.69"

        DB_HOST = credentials('DB_HOST')
        DB_NAME = credentials('DB_NAME')
        DB_PASS = credentials('DB_PASSWORD')
        DB_USER = "root"

        NGINX_CONF_PATH = "/home/sw_team_2/app/nginx/conf.d/app.conf"
    }

    stages {
        stage('1. Build') {
            steps {
                sh 'chmod +x ./gradlew'
                sh './gradlew clean build -x test'
            }
        }

        stage('2. SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh './gradlew sonar'
                }
            }
        }

        stage('3. Docker Build & Push') {
            steps {
                script {
                    docker.withRegistry('https://index.docker.io/v1/', 'docker-hub-key') {
                        def appImage = docker.build("${REPOSITORY}:${BUILD_NUMBER}")
                        appImage.push()
                        appImage.push('latest')
                    }
                }
            }
        }

        stage('4. Deploy (Blue/Green)') {
            steps {
                sshagent(credentials: ['deploy-server-ssh-key']) {
                    sh """
                        ssh -o StrictHostKeyChecking=no ${DEPLOY_SERVER} '
                            # 1. 현재 활성 환경 확인
                            CURRENT=\$(grep -o "sw_team_2_[a-z]*" ${NGINX_CONF_PATH} | head -1)

                            if [ "\$CURRENT" = "sw_team_2_blue" ]; then
                                TARGET="sw_team_2_green"
                                TARGET_PORT=8103
                            else
                                TARGET="sw_team_2_blue"
                                TARGET_PORT=8102
                            fi

                            # 2. 새 이미지 pull
                            docker pull ${REPOSITORY}:${BUILD_NUMBER}

                            # 3. 비활성 컨테이너 정리 후 새 버전 실행
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

                            # 4. Health Check (3초 간격, 최대 10회 = 30초)
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

                            # 5. Nginx 전환
                            sed -i "s/\$CURRENT/\$TARGET/" ${NGINX_CONF_PATH}
                            docker exec sw_team_2_nginx nginx -s reload

                            # 6. 이전 컨테이너 정리
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