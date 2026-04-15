#!/bin/bash

REPOSITORY=$1
DB_HOST=$2
DB_NAME=$3
DB_USER=$4
DB_PASS=$5
NGINX_CONF_PATH=$6

# 1. 현재 활성 환경 확인
CURRENT=$(grep -o "sw_team_2_[a-z]*" $NGINX_CONF_PATH | head -1)

if [ "$CURRENT" = "sw_team_2_blue" ]; then
    TARGET="sw_team_2_green"
    TARGET_PORT=8103
else
    TARGET="sw_team_2_blue"
    TARGET_PORT=8102
fi

# 2. 새 이미지 Pull
docker pull $REPOSITORY:latest

# 3. 비활성 컨테이너 정리 후 새 버전 실행
docker stop $TARGET || true
docker rm $TARGET || true

docker run -d --name $TARGET \
    --network sw_team_2_network \
    -p $TARGET_PORT:8080 \
    -e DB_HOST=$DB_HOST \
    -e DB_NAME=$DB_NAME \
    -e DB_USERNAME=$DB_USER \
    -e DB_PASSWORD=$DB_PASS \
    $REPOSITORY:latest

# 4. Health Check
for i in $(seq 1 10); do
    if curl -sf http://localhost:$TARGET_PORT/actuator/health > /dev/null 2>&1; then
        echo "Health check passed"
        break
    fi
    if [ $i -eq 10 ]; then
        echo "Health check failed"
        docker stop $TARGET || true
        docker rm $TARGET || true
        exit 1
    fi
    sleep 3
done

# 5. Nginx 트래픽 전환
sed -i "s/$CURRENT/$TARGET/" $NGINX_CONF_PATH
docker exec sw_team_2_nginx nginx -s reload

# 6. 이전 컨테이너 정리
docker stop $CURRENT || true
docker rm $CURRENT || true

echo "Deployed $TARGET successfully"