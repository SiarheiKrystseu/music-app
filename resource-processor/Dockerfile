FROM eclipse-temurin:21-jre-alpine

WORKDIR /music-app
COPY build/libs/resource-processor.jar resource-processor.jar

EXPOSE 8084

CMD ["java", "-jar", "resource-processor.jar"]
