FROM openjdk:17 AS builder
ENV APP_HOME=/usr/app/
WORKDIR $APP_HOME
RUN microdnf install findutils
COPY . .
RUN ./gradlew build

FROM openjdk:17
RUN microdnf install findutils
ENV ARTIFACT_NAME=toyou-backend.jar
ENV APP_HOME=/usr/app/
WORKDIR $APP_HOME
COPY --from=builder $APP_HOME/build/libs/$ARTIFACT_NAME .
EXPOSE 8102
CMD ["java","-jar",$ARTIFACT_NAME]
