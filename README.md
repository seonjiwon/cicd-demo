# CI/CD Demo - Blue/Green 무중단 배포 파이프라인

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

SonarQube 서버가 분석 결과를 처리한 뒤 Quality Gate 통과 여부를 webhook으로 Jenkins에 전달합니다. 실패 시 파이프라인이 중단되어 품질 기준을 충족하지 않는 코드는 배포되지 않습니다.

### 4단계: Docker Build & Push

Dockerfile을 기반으로 Spring Boot 애플리케이션 이미지를 빌드하고 Docker Hub에 push합니다.

### 5단계: Cleanup Jenkins Image

Docker Hub에 push 완료 후 Jenkins 서버의 로컬 이미지를 삭제하여 디스크 공간을 확보합니다.

### 6단계: Deploy (Blue/Green)

SSH로 배포 서버에 접속하여 Blue/Green 무중단 배포를 수행합니다. 상세 동작은 아래 섹션에서 설명합니다.

## 4. Blue/Green 배포 전략

### 동작 원리

동일한 Docker 이미지를 Blue(포트 8102)와 Green(포트 8103) 두 컨테이너로 분리하여 운영합니다. 배포 시 현재 트래픽을 받지 않는 쪽에 새 버전을 배포하고, 검증 후 Nginx가 트래픽을 전환합니다.