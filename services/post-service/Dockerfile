FROM maven:3.9.16-eclipse-temurin-21-alpine AS build
WORKDIR /workspace
COPY . .
RUN mvn --batch-mode --no-transfer-progress -pl :post-service -am package -DskipTests \
    && cp services/post-service/target/post-service-*.jar /workspace/application.jar

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /workspace/application.jar application.jar
USER app
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/application.jar"]
