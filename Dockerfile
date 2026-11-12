FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -ntp clean verify

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN groupadd --system safira && useradd --system --gid safira --home-dir /app safira
COPY --from=build --chown=safira:safira /app/target/safira-bank-0.0.1-SNAPSHOT.jar /app/app.jar
USER safira
EXPOSE 10000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]