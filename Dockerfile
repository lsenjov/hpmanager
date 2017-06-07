FROM java:8-alpine
MAINTAINER Your Name <you@example.com>

ADD target/uberjar/hpmanager.jar /hpmanager/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/hpmanager/app.jar"]
