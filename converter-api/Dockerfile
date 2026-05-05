FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY . .
RUN ./converter-api/build.sh

FROM eclipse-temurin:17-jre

WORKDIR /app
ENV CONVERTER_API_PORT=8080
ENV CONVERTER_JOBS_DIR=/data/jobs

COPY --from=build /workspace/converter-api/target/tuxguitar-converter-api-9.99-SNAPSHOT.jar /app/converter-api.jar

EXPOSE 8080
VOLUME ["/data"]

ENTRYPOINT ["sh", "-c", "java -Dconverter.api.port=${CONVERTER_API_PORT} -Dconverter.jobs.dir=${CONVERTER_JOBS_DIR} -jar /app/converter-api.jar"]