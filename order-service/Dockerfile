FROM maven:3.9.5-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY order-service/pom.xml order-service/
COPY common-domain/common-domain.pom.xml common-domain/
COPY order-service/src order-service/src
RUN mvn -f order-service/pom.xml -B -Dmaven.test.skip=true package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/order-service/target/*-SNAPSHOT.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java","-jar","/app/app.jar"]
