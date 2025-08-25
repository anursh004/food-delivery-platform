FROM maven:3.9.5-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY catalog-service/pom.xml catalog-service/
COPY common-domain/common-domain.pom.xml common-domain/
COPY catalog-service/src catalog-service/src
RUN mvn -f catalog-service/pom.xml -B -Dmaven.test.skip=true package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/catalog-service/target/*-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","/app/app.jar"]
