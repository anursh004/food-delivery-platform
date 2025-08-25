FROM maven:3.9.5-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY driver-simulator/pom.xml driver-simulator/
COPY common-domain/common-domain.pom.xml common-domain/
COPY driver-simulator/src driver-simulator/src
RUN mvn -f driver-simulator/pom.xml -B -Dmaven.test.skip=true package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/driver-simulator/target/*-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]
