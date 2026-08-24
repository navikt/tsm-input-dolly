FROM gcr.io/distroless/java25-debian13@sha256:d000221060080b5691b1521d3113f701bd3e0041c6b792a00cf9006d8d3b4ec0
WORKDIR /app
COPY build/libs/input-dolly-all.jar app.jar
COPY --chown=65532:65532 docs/ /app/docs/
ENV JAVA_OPTS="-Dlogback.configurationFile=logback.xml"
ENV TZ="Europe/Oslo"
EXPOSE 8080
USER nonroot
CMD [ "app.jar" ]
