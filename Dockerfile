FROM eclipse-temurin:21-jre

WORKDIR /app

RUN mkdir -p /app/newrelic
ADD https://download.newrelic.com/newrelic/java-agent/newrelic-agent/current/newrelic-agent.jar /app/newrelic/newrelic.jar
COPY newrelic.yml /app/newrelic/newrelic.yml

COPY build/libs/nt-manuscript-coverage-1.0.0.jar app.jar

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -javaagent:/app/newrelic/newrelic.jar -Dnewrelic.config.file=/app/newrelic/newrelic.yml -Dnewrelic.environment=${NEW_RELIC_ENVIRONMENT:-prod} -jar app.jar"]
