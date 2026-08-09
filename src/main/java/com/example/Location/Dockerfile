FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY src ./src

RUN mkdir out && \
    javac --add-modules jdk.httpserver \
    -d out \
    src/main/java/com/example/Location/LocationLinkServer.java

CMD ["java", "--add-modules", "jdk.httpserver", "-cp", "out", "com.example.Location.LocationLinkServer"]