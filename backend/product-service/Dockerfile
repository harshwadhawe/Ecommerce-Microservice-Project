# Self-contained build: no prior `mvn package` needed, so the image cannot silently ship
# whatever jar happened to be in target/.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app

# Unprivileged: a container escape should not land on root.
RUN useradd --system --uid 10001 spring
USER spring

COPY --from=build /build/target/product-service-1.0.0.jar app.jar

EXPOSE 8082

ENV JAVA_OPTS="-Xmx512m -Xms256m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
