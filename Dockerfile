FROM openjdk:17-oraclelinux8 as builder

USER root

RUN jlink \
    --module-path "$JAVA_HOME/jmods" \
    --add-modules java.compiler,java.sql,java.naming,java.management,java.instrument,java.rmi,java.desktop,jdk.internal.vm.compiler.management,java.xml.crypto,java.scripting,java.security.jgss,jdk.httpserver,java.net.http,jdk.naming.dns,jdk.crypto.cryptoki,jdk.unsupported \
    --verbose \
    --strip-debug \
    --compress 2 \
    --no-header-files \
    --no-man-pages \
    --output /opt/jre-minimal

USER app
#
## Now it is time for us to build our real image on top of an slim version of debian
#
FROM bitnami/minideb:bullseye
COPY --from=builder /opt/jre-minimal /opt/jre-minimal

ENV JAVA_HOME=/opt/jre-minimal
ENV PATH="$PATH:$JAVA_HOME/bin"

WORKDIR /

COPY build/libs/test-telemetry-generator-all.jar /

COPY tools/start.sh /
RUN chmod +x start.sh test-telemetry-generator-all.jar
CMD ["./start.sh"]