FROM eclipse-temurin:21-jre-alpine

WORKDIR /music-app
COPY build/libs/song-service.jar song-service.jar

EXPOSE 8081

CMD ["java", "-jar", "song-service.jar"]
