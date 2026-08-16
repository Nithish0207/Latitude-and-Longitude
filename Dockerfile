FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Change this path to your actual JAR location/name
COPY target/Sample-Project-1.0-SNAPSHOT.jar

ENV PORT=8080

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]