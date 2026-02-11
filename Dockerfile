# Usa l'immagine GraalVM ufficiale come base
FROM ghcr.io/graalvm/native-image-community:21 AS build
WORKDIR /app
COPY . .
# Genera il binario (Maven caricherà le dipendenze)
RUN ./mvnw native:compile -DskipTests

# Stage finale: estraiamo solo il binario
FROM scratch
COPY --from=build /app/target/timer /timer