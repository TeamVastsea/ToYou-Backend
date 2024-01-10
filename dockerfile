FROM openjdk:17 AS builder
WORKDIR /usr/app
RUN microdnf install findutils
COPY . .
RUN ./gradlew build
RUN ./gradlew rename

FROM openjdk:17
WORKDIR /usr/app
COPY --from=builder /usr/app/build/libs/ .
EXPOSE 8102
CMD java -jar toyou.jar
