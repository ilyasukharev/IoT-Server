FROM openjdk:19
EXPOSE 8000:8000
RUN mkdir /app
COPY ./build/libs/iot-server.jar /app/iot-server.jar
ENTRYPOINT ["java","-jar","/app/iot-server.jar"]