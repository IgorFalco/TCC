# Contexto de build = raiz do repositório (o build precisa de backend/ e database/migrations/)

# --- Build ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY backend/.mvn/ .mvn/
COPY backend/mvnw backend/pom.xml ./
RUN ./mvnw -B -q dependency:go-offline
COPY backend/src/ src/
COPY database/migrations/ /database/migrations/
RUN ./mvnw -B -q clean package -DskipTests

# --- Runtime ---
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
