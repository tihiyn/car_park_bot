FROM eclipse-temurin:21-jre
WORKDIR /app
COPY ./*.jar app.jar
COPY ./resources /app/
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]