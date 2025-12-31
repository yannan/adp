########################### Stage 0 ########################
ARG BUILD_IMAGE=acr.aishu.cn/ar/go.build:1.24.11-20251203
ARG BASE_IMAGE=acr.aishu.cn/public/ubuntu:22.04.20251014

FROM ${BUILD_IMAGE} AS builder

COPY ./server /go/src
COPY .netrc /root/.netrc
COPY id_rsa /root/.ssh/id_rsa

ARG SERVER_VERSION=3.2.0
ARG GOPROXY_URL="http://goproxy.aishu.cn"

ENV GOPROXY=${GOPROXY_URL}

RUN set -ex; \
    cd /go/src; \
    go mod download -x; \
    export flags="-X './server/version.ServerVersion=${SERVER_VERSION}'"; \
    CGO_ENABLED=1 go build -ldflags "-s -w $flags" -o ./bin/vega-gateway-pro-server;

########################### Stage 1 ########################
# Copy working directory to the actual release docker images
FROM ${BASE_IMAGE} AS prod

ARG UID=1000
ARG GID=1000

RUN groupadd -r -g $GID aishu && \
    useradd -r -u $UID -g $GID aishu && \
    apt-get update && apt-get install -y python3 python3-pip && \
    pip3 install sqlglot && \
    rm -rf /var/lib/apt/lists/*

COPY --from=builder --chown=$UID:$GID /go/src/bin /opt/vega-gateway-pro

ADD server/config/vega-gateway-pro-config.yaml /opt/vega-gateway-pro/config/
ADD server/common/rsa/private_key.pem /opt/vega-gateway-pro/rsa/
ADD server/locale/*.toml /opt/vega-gateway-pro/locale/

# 指定工作目录
WORKDIR /opt/vega-gateway-pro/

# Change user
USER $UID

# Expose port
EXPOSE 8097

# 指定启动命令
ENTRYPOINT [ "./vega-gateway-pro-server" ]
