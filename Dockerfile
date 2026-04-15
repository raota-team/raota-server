FROM eclipse-temurin:25.0.2_10-jre-jammy
WORKDIR /app
ENV TZ=Asia/Seoul
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_TOOL_OPTIONS="-Xms4g -Xmx4g"
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]