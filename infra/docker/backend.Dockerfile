# Build
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B dependency:go-offline
COPY src/ src/
RUN ./mvnw -B clean package -DskipTests

# Runtime
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/target/*.jar app.jar
# Migrations Flyway (módulo database) copiadas para dentro da imagem
COPY --from=build /src/target/classes/db/migration/ /app/db/migration/
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
