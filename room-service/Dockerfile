FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
RUN mvn -q -DskipTests package -pl room-service -am

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/room-service/target/*.jar app.jar
RUN groupadd -g 10001 hotelos && useradd -u 10001 -g hotelos -d /app -s /sbin/nologin hotelos \
    && chown -R hotelos:hotelos /app
USER 10001:10001
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
