FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
RUN mvn -q -DskipTests package -pl maintenance-service -am

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/maintenance-service/target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
