# Estágio 1: compilação e empacotamento Maven com JDK 21
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp clean package -DskipTests

# Estágio 2: imagem final enxuta apenas com JRE 21 e usuário não-root
FROM eclipse-temurin:21-jre

WORKDIR /app

RUN groupadd --system eventif && useradd --system --gid eventif eventif

COPY --from=build /workspace/target/*.jar /app/app.jar
RUN chown eventif:eventif /app/app.jar

USER eventif

EXPOSE 8080
ENV TZ=America/Fortaleza

ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-Duser.timezone=America/Fortaleza", "-jar", "/app/app.jar"]
