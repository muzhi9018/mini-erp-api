# syntax=docker/dockerfile:1

# 构建阶段使用 Maven 和 Java 25，运行镜像仅保留应用与 JRE。
FROM maven:3.10.0-eclipse-temurin-25-noble AS builder

WORKDIR /build

COPY pom.xml ./
COPY src ./src

# 缓存 Maven 依赖，源码修改后无需重新下载已解析的依赖。
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn -B -ntp package \
    && cp target/*.jar /build/service.jar

# Ubuntu 使用 glibc，适合项目依赖的 AWS CRT 原生库。
FROM eclipse-temurin:25-jre-noble

WORKDIR /service

ENV TZ=Asia/Shanghai \
    SPRING_PROFILES_ACTIVE=pro \
    JAVA_TOOL_OPTIONS="-Djava.security.egd=file:/dev/urandom -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

# 使用与宿主机一致的 UID/GID，并授予应用用户日志目录的写入权限。
RUN groupadd --system --gid 1001 app \
    && useradd --system --uid 1001 --gid 1001 --home-dir /service --shell /usr/sbin/nologin app \
    && mkdir -p /service/logs \
    && chown 1001:1001 /service/logs

COPY --from=builder --chown=1001:1001 /build/service.jar ./service.jar

USER 1001:1001

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/service/service.jar"]
