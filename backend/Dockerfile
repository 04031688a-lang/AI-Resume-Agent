# ============================================================
# AI Resume Agent 后端镜像
# 构建：docker build -t resume-agent-backend ./backend
# ============================================================
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# 先复制 pom 下载依赖，利用构建缓存
COPY pom.xml .
RUN mvn -q dependency:go-offline

COPY src ./src
RUN mvn -q clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/resume-agent-backend-0.1.0.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
