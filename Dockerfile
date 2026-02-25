FROM eclipse-temurin:25.0.2_10-jre-jammy
WORKDIR /app
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_TOOL_OPTIONS="-Xms512m -Xmx1g"
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
