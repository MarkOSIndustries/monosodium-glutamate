FROM timbru31/java-node:8-jdk as builder

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


FROM timbru31/java-node:8-jdk as runtime

ARG MSG_VERSION

WORKDIR /opt/msg
ENV MSG_HOME=/opt/msg
ENV MSG_VERSION=${MSG_VERSION}

COPY --from=builder /home/msg/schemas ./bin/schemas
COPY --from=builder /home/msg/kat/build/libs/*.jar ./bin/kat.jar
COPY --from=builder /home/msg/kgb/build/libs/*.jar ./bin/kgb.jar
COPY --from=builder /home/msg/qs/build/libs/*.jar ./bin/qs.jar
COPY --from=builder /home/msg/*.js ./bin/
COPY --from=builder /home/msg/proto ./bin/proto

COPY docker/bin/kat.sh /usr/local/bin/kat
COPY docker/bin/kgb.sh /usr/local/bin/kgb
COPY docker/bin/qs.sh /usr/local/bin/qs
COPY docker/bin/proto.sh /usr/local/bin/proto
COPY docker/bin/version.sh /usr/local/bin/version

# KGB default port
EXPOSE 8082
# QS default port
EXPOSE 8083

RUN mkdir /home/schemas
ENV PROTO_HOME=/home/schemas

RUN mkdir /home/root
WORKDIR /home/root

RUN echo 'if ! [[ -z "$KAFKA_BROKERS" ]]; then export COMPLETIONS_KAFKA_TOPICS=${MSG_HOME}/topics.txt; (kat topics > $COMPLETIONS_KAFKA_TOPICS 2> /dev/null &); fi' >> /root/.bashrc
RUN (cd $MSG_HOME/bin/ && _KAT_COMPLETE=bash java -jar ./kat.jar >> /root/.bashrc)
RUN (cd $MSG_HOME/bin/ && node ./proto/ completion >> /root/.bashrc)
RUN echo 'export PS1="\[\033[01;32m\]msg\[\033[02;37m\]:\[\033[00;01;90m\]v$MSG_VERSION\[\033[02;37m\]:\[\033[00;01;34m\]\w\[\033[02;37m\]\$\[\033[00m\] "' >> /root/.bashrc
CMD bash