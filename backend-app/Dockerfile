FROM eclipse-temurin:11-alpine AS builder

COPY . /app
WORKDIR /app

RUN ./gradlew --no-daemon build

FROM eclipse-temurin:11-jre-alpine AS runner

RUN mkdir -p /app
WORKDIR /app
COPY --from=builder /app/build/libs/scc-backend-0.0.1-SNAPSHOT.jar .

EXPOSE 8080

CMD java -jar scc-backend-0.0.1-SNAPSHOT.jar