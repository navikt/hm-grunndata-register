FROM gcr.io/distroless/java17-debian12:nonroot
WORKDIR /app
ENV TZ="Europe/Oslo"
EXPOSE 8080
COPY build/libs/hm-grunndata-register-all.jar ./app.jar
CMD ["-jar", "app.jar"]