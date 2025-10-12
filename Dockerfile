# syntax=docker/dockerfile:1.7-labs

FROM ubuntu:24.04 AS backend-builder
RUN apt update && apt install gnuplot graphviz cmake python3-graphviz gcc g++ -y
COPY --chown=builder backend/external/userver/scripts/docs/en/deps/ubuntu-24.04.md /tmp/deps
RUN apt install --allow-downgrades -y $(cat /tmp/deps | tr '\n' ' ')
RUN useradd -s /bin/bash builder
USER builder
WORKDIR /home/builder/repo
COPY --chown=builder backend .
RUN --mount=type=cache,target=/home/builder/repo/build,uid=1001 \
    cmake -DCMAKE_BUILD_TYPE=Release -Bbuild && cmake --build build -j8 \
    && cp /home/builder/repo/build/nons outputs/nons

FROM eclipse-temurin:11 AS builder
RUN useradd -m -s /bin/bash builder
USER builder
WORKDIR /home/builder/repo
COPY --exclude=nginx --exclude=backend  --chown=builder . .
RUN --mount=type=cache,target=/home/builder/.gradle,uid=1001 \
    --mount=type=cache,target=/home/builder/repo/.gradle,uid=1001 \
    --mount=type=cache,target=/home/builder/repo/build,uid=1001 \
    --mount=type=cache,target=/home/builder/repo/shared/build,uid=1001 \
    --mount=type=cache,target=/home/builder/repo/server/build,uid=1001 \
    --mount=type=cache,target=/home/builder/repo/client/build,uid=1001 \
    ./gradlew :server:installDist :client:wasmJsBrowserDistribution \
    && cp -r /home/builder/repo/server/build/install/server outputs/server \
    && cp -r /home/builder/repo/client/build/dist/wasmJs/productionExecutable outputs/client

FROM eclipse-temurin:11 AS server
RUN apt update && apt install gnuplot graphviz python3-graphviz gcc g++ -y
COPY --chown=builder backend/external/userver/scripts/docs/en/deps/ubuntu-24.04.md /tmp/deps
RUN apt install --allow-downgrades -y $(cat /tmp/deps | tr '\n' ' ')
COPY --from=builder /home/builder/repo/outputs/server /server
COPY --from=backend-builder /home/builder/repo/outputs/nons /server/bin/nons
COPY backend/config /server/
ENV NONS_PATH=/server/bin/nons
ENV NONS_WEBSOCKET_STATIC_CONFIG_PATH=/server/config/static-config.yml
RUN useradd -s /bin/bash server
USER server
WORKDIR /tmp/server
CMD ["/server/bin/server"]

FROM nginxinc/nginx-unprivileged:1.29.1-alpine3.22-perl AS client
RUN rm /etc/nginx/conf.d/default.conf
WORKDIR /client
COPY --from=builder --exclude=*.map /home/builder/repo/outputs/client .
COPY --chown=nginx nginx/10-app.conf /etc/nginx/templates/10-app.conf.template
ARG CLIENT_PORT
ARG SERVER_ADDRESS
