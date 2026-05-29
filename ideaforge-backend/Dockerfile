# IdeaForge API — build JAR locally or in CI, then image copies the fat JAR.
# Build:  mvn -q -DskipTests package
# Run:    docker run -p 8080:8080 -e DB_URL=... -e JWT_SECRET=... ideaforge-api

FROM openjdk:17-slim
WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
