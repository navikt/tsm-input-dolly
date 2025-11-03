FROM gcr.io/distroless/java21-debian12@sha256:1fcdb697531e4e1d5acf7e1230a8f007d683321c71f6e4f4063603ecb1502849
WORKDIR /app
COPY build/libs/input-dolly-all.jar app.jar
COPY --chown=65532:65532 docs/ /app/docs/
ENV JAVA_OPTS="-Dlogback.configurationFile=logback.xml"
ENV TZ="Europe/Oslo"
EXPOSE 8080
USER nonroot
CMD [ "app.jar" ]
