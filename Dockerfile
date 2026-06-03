FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY pom.xml .
COPY config ./config
COPY src ./src
RUN mvn -DskipTests package

FROM eclipse-temurin:17-jre

WORKDIR /app

RUN groupadd --system aftersale \
    && useradd --system --gid aftersale --home-dir /app --shell /usr/sbin/nologin aftersale \
    && chown aftersale:aftersale /app

COPY --from=build --chown=aftersale:aftersale /workspace/target/*.jar /app/app.jar

ENV JAVA_OPTS=""

EXPOSE 8080
USER aftersale
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
