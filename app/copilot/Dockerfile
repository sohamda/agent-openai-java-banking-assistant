FROM mcr.microsoft.com/openjdk/jdk:17-ubuntu@sha256:3be0c6cf5878bd9a7ff56bcbb5e4f0d00a7799dfc39125cabd2be2314405d297 AS build

WORKDIR /workspace/app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY copilot-backend copilot-backend
COPY semantickernel-openapi-plugin semantickernel-openapi-plugin

RUN chmod +x ./mvnw
# Convert CRLF to LF
RUN sed -i 's/\r$//' ./mvnw
RUN ./mvnw package -DskipTests
RUN mkdir -p copilot-backend/target/dependency && (cd copilot-backend/target/dependency; jar -xf ../*.jar)

#install curl
RUN apt-get update && apt-get install -y --no-install-recommends curl && apt-get clean && rm -rf /var/lib/apt/lists/*
RUN curl -LJ -o applicationinsights-agent-3.5.4.jar https://github.com/microsoft/ApplicationInsights-Java/releases/download/3.5.4/applicationinsights-agent-3.5.4.jar \
    && echo "4d49a6a3bfc223cf36d5e41b430e1a732d27b920f29619b34fab933c3f7ecc08  applicationinsights-agent-3.5.4.jar" | sha256sum -c -
COPY applicationinsights.json .

#for production deployment use mcr.microsoft.com/openjdk/jdk:17-distroless
FROM mcr.microsoft.com/openjdk/jdk:17-distroless@sha256:ffa65843530d6d1fb04c3fc3778bb37867ab5b34f76a647650d49bab6ddb8f8b

ARG DEPENDENCY=/workspace/app/copilot-backend/target/dependency/
COPY --from=build ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=build ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=build ${DEPENDENCY}/BOOT-INF/classes /app
COPY --from=build /workspace/app/applicationinsights-agent-3.5.4.jar /app
COPY --from=build /workspace/app/applicationinsights.json /app
EXPOSE 8080

USER 65534
ENTRYPOINT ["java","-javaagent:/app/applicationinsights-agent-3.5.4.jar", "-XX:MaxRAMPercentage=70", "-XX:+UseParallelGC", "-XX:ActiveProcessorCount=2", "-cp","app:app/lib/*","com.microsoft.openai.samples.assistant.CopilotApplication"]