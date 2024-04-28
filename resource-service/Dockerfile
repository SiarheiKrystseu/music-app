FROM eclipse-temurin:21-jre-alpine

WORKDIR /music-app
COPY build/libs/resource-service.jar resource-service.jar

EXPOSE 8082

CMD ["java", "-jar", "resource-service.jar"]
