# syntax=docker/dockerfile:1
#
# Um Dockerfile para os oito serviços. O módulo vem por argumento:
#
#   docker build --build-arg MODULO=reserva-service -t cinepass/reserva-service .
#
# No docker-compose cada serviço passa o seu MODULO. O cache do Maven é montado e
# compartilhado entre os builds (sharing=locked serializa o acesso): as dependências
# são baixadas uma vez, e não oito.

FROM maven:3.9-eclipse-temurin-25 AS build
ARG MODULO
WORKDIR /src
COPY . .
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn -B -q -pl ${MODULO} -am package -DskipTests

FROM eclipse-temurin:25-jre-alpine
ARG MODULO
# Sem root: o processo Java não precisa de privilégio nenhum dentro do container.
RUN addgroup -S cinepass && adduser -S cinepass -G cinepass
WORKDIR /app
COPY --from=build /src/${MODULO}/target/${MODULO}-*.jar app.jar
USER cinepass
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -Dfile.encoding=UTF-8"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
