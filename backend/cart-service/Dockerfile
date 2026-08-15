FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/cart-service-1.0.0.jar app.jar

EXPOSE 8083

ENV JAVA_OPTS="-Xmx512m -Xms256m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]