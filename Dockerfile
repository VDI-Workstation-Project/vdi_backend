# Backend Dockerfile
FROM eclipse-temurin:21-jdk-alpine

# 작업 디렉토리 설정
WORKDIR /app

# 타임존 설정
RUN apk add --no-cache tzdata
ENV TZ=Asia/Seoul

# JAR 파일 복사
COPY build/libs/*.jar app.jar

# Spring Profile 설정 및 실행
ENTRYPOINT ["java", \
            "-jar", \
            "-Dspring.profiles.active=prod", \
            "-Dserver.port=8081", \
            "app.jar"]

# Test trigger CI/CD pipeline 6