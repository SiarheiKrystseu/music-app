FROM eclipse-temurin:21-jre-alpine

WORKDIR /music-app
COPY build/libs/api-gateway.jar api-gateway.jar

EXPOSE 8083

CMD ["java", "-jar", "api-gateway.jar"]
