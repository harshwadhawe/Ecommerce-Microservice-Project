FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/product-service-1.0.0.jar app.jar

EXPOSE 8082

ENV JAVA_OPTS="-Xmx512m -Xms256m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]