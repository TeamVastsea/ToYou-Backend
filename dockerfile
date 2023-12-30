FROM openjdk:17 AS TEMP_BUILD_IMAGE
ENV APP_HOME=/usr/app/
WORKDIR $APP_HOME
COPY build.gradle settings.gradle gradlew $APP_HOME
COPY gradle $APP_HOME/gradle
RUN ./gradlew build
COPY . .
RUN ./gradlew build

FROM openjdk:17
ENV ARTIFACT_NAME=toyou-backend.jar
ENV APP_HOME=/usr/app/
WORKDIR $APP_HOME
COPY --from=TEMP_BUILD_IMAGE $APP_HOME/build/libs/$ARTIFACT_NAME .
EXPOSE 8102
CMD ["java","-jar",$ARTIFACT_NAME]
