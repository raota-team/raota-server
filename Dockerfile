FROM eclipse-temurin:25.0.2_10-jre-jammy AS builder
WORKDIR /builder
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} application.jar
# 의존성과 애플리케이션 코드를 레이어로 나눠, 코드만 바뀐 커밋은 작은 application 레이어만 다시 push한다.
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

FROM eclipse-temurin:25.0.2_10-jre-jammy AS app
WORKDIR /app
ENV TZ=Asia/Seoul
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
# 자주 바뀌지 않는 레이어부터 복사해야 앞 레이어의 캐시가 유지된다.
COPY --from=builder /builder/extracted/dependencies/ ./
COPY --from=builder /builder/extracted/spring-boot-loader/ ./
COPY --from=builder /builder/extracted/snapshot-dependencies/ ./
COPY --from=builder /builder/extracted/application/ ./
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_TOOL_OPTIONS="-Xms4g -Xmx4g"
ARG GIT_COMMIT=unknown
ENV APP_RAG_EVALUATION_SERVER_COMMIT=${GIT_COMMIT}
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/application.jar"]
