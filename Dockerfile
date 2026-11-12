FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-27
WORKDIR /app
ENV TZ="Europe/Oslo"
EXPOSE 8080
COPY build/libs/hm-grunndata-register-all.jar ./app.jar
CMD ["-jar", "app.jar"]
