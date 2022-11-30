FROM navikt/java:17
USER root
RUN apt-get update && apt-get install -y curl
USER apprunner
COPY scripts/init-env.sh /init-scripts/init-env.sh
COPY build/libs/hm-grunndata-register-all.jar ./app.jar
ENV JAVA_OPTS="-Xms256m -Xmx1024m"
