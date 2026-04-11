FROM eclipse-temurin:21-jre

WORKDIR /app

COPY build/libs/nt-manuscript-coverage-1.0.0.jar app.jar

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
