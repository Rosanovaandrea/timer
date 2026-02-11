USER root
RUN microdnf install -y gcc glibc-devel zlib-devel libstdc++-devel
# Usa l'immagine GraalVM ufficiale come base
FROM ghcr.io/graalvm/native-image-community:21 AS build
WORKDIR /app
COPY . .
# Genera il binario (Maven caricherà le dipendenze)
RUN ./mvnw native:compile -DskipTests-image.xmx=6g -e -X

# Stage finale: estraiamo solo il binario
FROM scratch
COPY --from=build /app/target/timer /timer