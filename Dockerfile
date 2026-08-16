FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the JAR into the image as app.jar
COPY target/Sample-Project-1.0-SNAPSHOT.jar app.jar

ENV PORT=8080

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]