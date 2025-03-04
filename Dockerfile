FROM ubuntu/jre:17-22.04_edge

WORKDIR /app
COPY target/crawler.jar .

ENTRYPOINT ["java", "-jar", "crawler.jar"]
