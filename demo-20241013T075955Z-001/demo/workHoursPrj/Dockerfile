FROM eclipse-temurin:18-jdk-alpine
VOLUME /tmp
COPY target/demo-0.0.1-SNAPSHOT.jar ascii-art-generator.jar
ENTRYPOINT ["java","-jar","/ascii-art-generator.jar"]