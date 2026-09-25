# syntax=docker/dockerfile:1

FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /build

COPY pom.xml .
RUN mvn -B dependency:go-offline -DskipTests

COPY src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine AS runtime

RUN apk upgrade --no-cache libexpat \
    && apk del coreutils \
    && addgroup -S app \
    && adduser -S app -G app

WORKDIR /app

COPY --from=build /build/target/temple-platform-*.jar /app/app.jar

USER app

EXPOSE 8080

CMD ["java", "-jar", "/app/app.jar"]
