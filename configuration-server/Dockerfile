FROM eclipse-temurin:21-jre-alpine

WORKDIR /music-app
COPY build/libs/configuration-server.jar configuration-server.jar

EXPOSE 8084

CMD ["java", "-jar", "configuration-server.jar"]
