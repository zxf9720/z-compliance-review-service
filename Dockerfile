FROM eclipse-temurin:25-jdk AS build

WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -DskipTests dependency:go-offline

COPY src/ src/
RUN ./mvnw -B package

FROM eclipse-temurin:25-jre

WORKDIR /app

RUN useradd --system --uid 10001 spring
USER spring

COPY --from=build /workspace/target/z-compliance-review-service-*.jar app.jar

EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
