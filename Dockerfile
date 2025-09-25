# syntax=docker/dockerfile:1.7-labs

FROM ubuntu:24.04 AS backend-builder
RUN apt update && apt install gnuplot graphviz cmake python3-graphviz gcc g++ -y
RUN useradd -s /bin/bash builder
USER builder
WORKDIR /home/builder/repo
COPY --chown=builder backend .
RUN cmake -DCMAKE_BUILD_TYPE=Release -Bbuild && cmake --build build -j8

FROM eclipse-temurin:11 AS builder
RUN useradd -m -s /bin/bash builder
USER builder
WORKDIR /home/builder/repo
COPY --exclude=nginx --exclude=backend  --chown=builder . .
RUN --mount=type=cache,target=/home/builder/.gradle,uid=1001 ./gradlew :server:installDist :client:wasmJsBrowserDistribution

FROM eclipse-temurin:11 AS server
RUN apt update && apt install gnuplot graphviz python3-graphviz gcc g++ -y
COPY --from=builder /home/builder/repo/server/build/install/server /server
COPY --from=backend-builder /home/builder/repo/build/nons /server/bin/nons
ENV NONS_PATH=/server/bin/nons
RUN useradd -s /bin/bash server
USER server
WORKDIR /tmp/server
CMD ["/server/bin/server"]

FROM nginxinc/nginx-unprivileged:1.29.1-alpine3.22-perl AS client
RUN rm /etc/nginx/conf.d/default.conf
WORKDIR /client
COPY --from=builder --exclude=*.map /home/builder/repo/client/build/dist/wasmJs/productionExecutable .
COPY --chown=nginx nginx/10-app.conf /etc/nginx/templates/10-app.conf.template
ARG CLIENT_PORT
ARG SERVER_ADDRESS
