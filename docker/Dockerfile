FROM eclipse-temurin:11.0.23_9-jdk AS builder
RUN apt update && apt install -y nodejs npm

WORKDIR /home/msg

# First get gradle installed and cached
COPY .gradle .gradle
COPY gradle gradle
COPY gradlew gradlew
RUN chmod +x ./gradlew
RUN ./gradlew

# Pull in MSG code
COPY . .

# gradle build MSG kotlin components
RUN ./gradlew clean build

# npm install MSG javascript components
RUN (cd proto && npm install)
RUN (cd p2p && npm install)
RUN (cd reduce && npm install)

FROM eclipse-temurin:11.0.23_9-jre AS runtime
RUN apt update && apt install -y nodejs netcat nano jq

ARG MSG_VERSION

WORKDIR /opt/msg
ENV MSG_HOME=/opt/msg
ENV MSG_VERSION=${MSG_VERSION}

COPY --from=builder /home/msg/schemas ./bin/schemas
COPY --from=builder /home/msg/kat/build/install/kat/*.jar ./bin/kat.jar
COPY --from=builder /home/msg/kat/build/install/kat/dependencies ./bin/dependencies
COPY --from=builder /home/msg/kgb/build/install/kgb/*.jar ./bin/kgb.jar
COPY --from=builder /home/msg/kgb/build/install/kgb/dependencies ./bin/dependencies
COPY --from=builder /home/msg/qs/build/install/qs/*.jar ./bin/qs.jar
COPY --from=builder /home/msg/qs/build/install/qs/dependencies ./bin/dependencies
COPY --from=builder /home/msg/*.js ./bin/
COPY --from=builder /home/msg/proto/*.js ./bin/proto/
COPY --from=builder /home/msg/proto/node_modules ./bin/proto/node_modules
COPY --from=builder /home/msg/p2p/*.js ./bin/p2p/
COPY --from=builder /home/msg/p2p/node_modules ./bin/p2p/node_modules
COPY --from=builder /home/msg/reduce/*.js ./bin/reduce/
COPY --from=builder /home/msg/reduce/node_modules ./bin/reduce/node_modules

COPY docker/bin/kat.sh /usr/local/bin/kat
COPY docker/bin/kgb.sh /usr/local/bin/kgb
COPY docker/bin/qs.sh /usr/local/bin/qs
COPY docker/bin/proto.sh /usr/local/bin/proto
COPY docker/bin/p2p.sh /usr/local/bin/p2p
COPY docker/bin/reduce.sh /usr/local/bin/reduce
COPY docker/bin/version.sh /usr/local/bin/version

# KGB default port
EXPOSE 8082
# QS default port
EXPOSE 8083

RUN mkdir /home/schemas
ENV PROTO_HOME=/home/schemas

RUN mkdir /home/root
WORKDIR /home/root

RUN echo 'export COMPLETIONS_KAFKA_TOPICS=${MSG_HOME}/topics.txt; (kat topics > $COMPLETIONS_KAFKA_TOPICS 2> /dev/null &);' >> /root/.bashrc
RUN (cd $MSG_HOME/bin/ && _KAT_COMPLETE=bash java -jar ./kat.jar >> /root/.bashrc)
RUN (cd $MSG_HOME/bin/ && node ./proto/ completion >> /root/.bashrc)
RUN echo 'export PS1="\[\033[01;32m\]msg\[\033[02;37m\]:\[\033[00;01;90m\]v$MSG_VERSION\[\033[02;37m\]:\[\033[00;01;34m\]\w\[\033[02;37m\]\$\[\033[00m\] "' >> /root/.bashrc
CMD bash
