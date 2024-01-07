FROM gradle:jdk17 AS builder
ENV APP_HOME=/usr/app/
WORKDIR $APP_HOME
COPY build.gradle settings.gradle $APP_HOME
COPY gradle $APP_HOME/gradle
RUN gradle build
COPY . .
RUN gradle build

FROM openjdk:17
RUN microdnf install findutils
ENV ARTIFACT_NAME=toyou-backend.jar
ENV APP_HOME=/usr/app/
WORKDIR $APP_HOME
COPY --from=builder $APP_HOME/build/libs/$ARTIFACT_NAME .
EXPOSE 8102
CMD ["java","-jar",$ARTIFACT_NAME]
