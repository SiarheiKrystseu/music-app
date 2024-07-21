FROM eclipse-temurin:21-jre-alpine

# Install dockerize
ENV DOCKERIZE_VERSION v0.6.1
RUN wget https://github.com/jwilder/dockerize/releases/download/$DOCKERIZE_VERSION/dockerize-alpine-linux-amd64-$DOCKERIZE_VERSION.tar.gz \
    && tar -C /usr/local/bin -xzvf dockerize-alpine-linux-amd64-$DOCKERIZE_VERSION.tar.gz \
    && rm dockerize-alpine-linux-amd64-$DOCKERIZE_VERSION.tar.gz

WORKDIR /music-app
COPY build/libs/api-gateway.jar api-gateway.jar

EXPOSE 8083

CMD dockerize -wait tcp://configuration-server:8084 -timeout 1m java -jar api-gateway.jar
