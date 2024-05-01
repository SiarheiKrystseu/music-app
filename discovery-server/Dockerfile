FROM eclipse-temurin:21-jre-alpine

WORKDIR /music-app
COPY build/libs/discovery-server.jar discovery-server.jar

EXPOSE 8761

CMD ["java", "-jar", "discovery-server.jar"]
