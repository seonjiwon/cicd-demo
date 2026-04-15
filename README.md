# CI/CD Demo - Blue/Green 무중단 배포 파이프라인

![202604151221-ezgif com-video-to-gif-converter](https://github.com/user-attachments/assets/30538ecc-0b94-44b0-a060-ef7f07045d2c)


## 1. 프로젝트 개요

Jenkins와 SonarQube를 활용한 CI/CD 파이프라인을 구축하고, Nginx 기반 Blue/Green 배포 전략으로 무중단 배포를 구현한 프로젝트입니다.

### 사용 기술 스택

| 구분 | 기술 |
|------|------|
| Backend | Spring Boot 3.5, Java 17, Spring Data JPA |
| Database | MySQL 8.0 |
| CI/CD | Jenkins, SonarQube (Community), Jacoco, Docker Hub |
| 배포 | Docker, Nginx (리버스 프록시 + Blue/Green 전환) |
| 코드 관리 | GitHub, Webhook (ngrok) |

## 2. 아키텍처
<img width="1169" height="451" alt="Image" src="https://github.com/user-attachments/assets/9f357320-f4ac-4b5a-9c51-793b35453182" />

- **CI 서버 (172.21.33.26):** Jenkins(1225), SonarQube(1224), PostgreSQL(5432)
- **배포 서버 (172.21.33.69):** MySQL(8101), Nginx(8100), Blue(8102), Green(8103)
- **외부 서비스:** Docker Hub, GitHub

## 3. CI/CD 파이프라인 흐름

개발자가 GitHub main 브랜치에 push하면 webhook을 통해 Jenkins 파이프라인이 자동 실행됩니다.

### 1단계: Build & Test

Gradle로 프로젝트를 빌드하고 테스트를 실행합니다. Jacoco가 테스트 커버리지 리포트를 생성하며, 테스트 실패 시 파이프라인이 중단됩니다.

### 2단계: SonarQube Analysis

Jenkins에서 SonarQube Gradle 플러그인(`./gradlew sonar`)을 실행합니다. Scanner가 SonarQube 서버에서 언어 분석기와 Quality Profile을 다운로드한 뒤, Jenkins 로컬에서 코드를 분석하고 결과 리포트를 SonarQube 서버로 전송합니다.

### 3단계: Quality Gate

SonarQube 서버가 분석 결과를 처리한 뒤 Quality Gate 통과 여부를 webhook으로 Jenkins에 전달합니다. 

<img width="1558" height="833" alt="image (2)" src="https://github.com/user-attachments/assets/746df8c3-092a-4598-bffd-ade9633e6505" />


> 실패 시 파이프라인이 중단되어 품질 기준을 충족하지 않는 코드는 배포되지 않습니다.

<img width="1622" alt="image (3)" src="https://github.com/user-attachments/assets/43c9febf-4144-42de-aefa-480b6a4648dd" />
<img width="500" alt="image (4)" src="https://github.com/user-attachments/assets/d39f484b-35cd-4880-b68b-d6b919b5608b" />
<img width="500" alt="image (5)" src="https://github.com/user-attachments/assets/c8f8250e-6336-4846-a373-08d77b6790e9" />




### 4단계: Docker Build & Push

Dockerfile을 기반으로 Spring Boot 애플리케이션 이미지를 빌드하고 Docker Hub에 push합니다.

### 5단계: Cleanup Jenkins Image

Docker Hub에 push 완료 후 Jenkins 서버의 로컬 이미지를 삭제하여 디스크 공간을 확보합니다.

### 6단계: Deploy (Blue/Green)

SSH로 배포 서버에 접속하여 Blue/Green 무중단 배포를 수행합니다. 상세 동작은 아래 섹션에서 설명합니다.

## 4. Blue/Green 배포 전략

### 동작 원리

동일한 Docker 이미지를 Blue(포트 8102)와 Green(포트 8103) 두 컨테이너로 분리하여 운영합니다. 배포 시 현재 트래픽을 받지 않는 쪽에 새 버전을 배포하고, 검증 후 Nginx가 트래픽을 전환합니다.

배포 전:

사용자 → Nginx(8100) → Blue(8102) [v1] ✅ 활성
Green(8103)      ❌ 비활성

배포 중:

사용자 → Nginx(8100) → Blue(8102) [v1] ✅ 트래픽 유지
Green(8103) [v2] 🔄 기동 + Health Check

전환 후:

사용자 → Nginx(8100) → Blue(8102) [v1] ❌ 정리
Green(8103) [v2] ✅ 활성

### 전환 메커니즘 (Nginx)

Nginx의 `app.conf`에서 upstream 서버를 정의하고, 배포 시 `sed` 명령으로 upstream 대상을 교체한 뒤 `nginx -s reload`로 설정을 반영합니다. reload는 기존 연결을 유지한 채 새 설정을 적용하므로 다운타임이 발생하지 않습니다.

```nginx
upstream backend {
    server sw_team_2_blue:8080;  # 이 값을 sw_team_2_green으로 교체
}
```

### 롤백 방법

이전 버전의 컨테이너를 다시 띄우고 Nginx upstream을 되돌린 뒤 reload하면 즉시 롤백됩니다.

## 5. 인프라 구성

### Docker 네트워크

모든 컨테이너는 `sw_team_2_network`에 연결되어 컨테이너 이름으로 서로 통신합니다.

### MySQL

`docker-compose.yml`로 관리하며 배포와 무관하게 항상 실행됩니다.

```yaml
services:
  mysql:
    image: mysql:8.0
    container_name: sw_team_2_mysql
    environment:
      MYSQL_ROOT_PASSWORD: 1234
      MYSQL_DATABASE: todo
    ports:
      - "8101:3306"
    volumes:
      - sw_team_2_mysql_data:/var/lib/mysql
    networks:
      - sw_team_2_network
    restart: unless-stopped
```

### Nginx

Blue 컨테이너가 실행된 후 `docker run`으로 별도 실행합니다. 설정 파일은 호스트에서 마운트합니다.

### Dockerfile

```dockerfile
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## 6. 트러블슈팅

| 문제 | 원인 | 해결 |
|------|------|------|
| SonarQube 컨테이너 반복 종료 | `vm.max_map_count` 기본값(65530)이 Elasticsearch 최소 요구치(262144) 미달 | `sudo sysctl -w vm.max_map_count=262144` |
| SonarQube "unsupported URI" | 컨테이너 이름(`seonjiwon_sonarqube_1`)의 언더스코어가 HTTP URI 표준 위반 | `docker network connect --alias seonjiwon-sonarqube`로 하이픈 기반 별칭 부여 |
| Jenkins에서 Docker 권한 오류 | Jenkins 컨테이너가 Docker 소켓 접근 권한 없음 | `docker exec -u root jenkins chmod 666 /var/run/docker.sock` |
| 테스트 빌드 실패 (DB 연결) | Jenkins 환경에서 MySQL 미존재 | 테스트용 `src/test/resources/application.yml`에 H2 인메모리 DB 설정 분리 |
