FROM ghcr.io/navikt/baseimages/temurin:17
USER root
USER apprunner
COPY build/libs/hm-grunndata-register-all.jar ./app.jar
