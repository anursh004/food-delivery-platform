FROM maven:3.9.5-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY logistics-service/pom.xml logistics-service/
COPY common-domain/pom.xml common-domain/
COPY logistics-service/src logistics-service/src
RUN mvn -f logistics-service/pom.xml -B -Dmaven.test.skip=true package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/logistics-service/target/*-SNAPSHOT.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java","-jar","/app/app.jar"]
