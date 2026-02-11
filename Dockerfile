
# Usa l'immagine GraalVM ufficiale come base
FROM ghcr.io/graalvm/native-image-community:25 AS build
WORKDIR /app
COPY . .

RUN microdnf install -y gcc glibc-devel zlib-devel libstdc++-devel || true
RUN microdnf install -y sqlite-devel || true
# Genera il binario (Maven caricherà le dipendenze)
RUN ./mvnw native:compile -DskipTests -Dnative-image.xmx=6g -e -X

# Stage finale: estraiamo solo il binario
FROM scratch
COPY --from=build /app/target/timer /timer
