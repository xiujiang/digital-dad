# ============================================================
# 数字爸爸 - Docker 镜像构建（多阶段）
# ============================================================

# 阶段1：构建
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# 复制 pom 和源码并构建
COPY pom.xml .
COPY src ./src
RUN mvn package -DskipTests -B -q

# 阶段2：运行
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 创建非 root 用户
RUN addgroup -g 1000 appgroup && \
    adduser -u 1000 -G appgroup -D appuser

# 从构建阶段复制 jar
COPY --from=builder /app/target/digital-dad-*.jar app.jar

# 使用非 root 用户运行
USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
