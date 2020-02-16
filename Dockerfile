FROM openjdk:11-jdk-slim as builder
ENV DEBIAN_FRONTEND noninteractive
RUN apt-get update && \
    apt-get install -y --no-install-recommends apt-transport-https apt-utils bc dirmngr gnupg && \
    echo "deb https://dl.bintray.com/sbt/debian /" | tee -a /etc/apt/sources.list.d/sbt.list && \
    apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 2EE0EA64E40A89B84B2DF73499E82A75642AC823 && \
    # seems that dash package upgrade is broken in Debian, so we hold it's version before update
    echo "dash hold" | dpkg --set-selections && \
    apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y --no-install-recommends sbt
#COPY ["build.sbt", "lock.sbt", "/ergo/"]
COPY ["build.sbt", "/ergoproxy/"]
COPY ["project", "/ergoproxy/project"]
RUN sbt update
COPY . /ergoproxy
WORKDIR /ergoproxy
RUN sbt assembly
RUN mv `find . -name ergo-proxy-*.jar` /ergoproxy.jar
CMD ["java", "-jar", "/ergoproxy.jar"]

FROM openjdk:11-jre-slim
#LABEL maintainer="Andrey Andreev <andyceo@yandex.ru> (@andyceo)"
RUN adduser --disabled-password --home /home/ergo --uid 9052 --gecos "ErgoPlatform" ergo && \
    install -m 0750 -o ergo -g ergo  -d /home/ergo/proxy
COPY --from=builder /ergoproxy.jar /home/ergo/ergoproxy.jar
USER ergo
EXPOSE 9000
WORKDIR /home/ergo
VOLUME ["/home/ergo/proxy"]
#ENTRYPOINT ["java", "-jar", "-D\"config.file\"=/home/ergo/proxy/proxy.conf", "/home/ergo/ergoproxy.jar"]
ENTRYPOINT java -jar -Dconfig.file=/home/ergo/proxy/proxy.conf /home/ergo/ergoproxy.jar
CMD [""]
