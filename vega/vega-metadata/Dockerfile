FROM acr.aishu.cn/af/openjdk:8u342-bullseye-all
ENV TZ=Asia/Shanghai
ENV JAVA_OPTS="-Xms512m -Xmx1024m"
RUN ln -sf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone && \
    mkdir -p /metadata-manage/logs && \
    chmod 777 /metadata-manage/logs && \
    sed -i 's/http:\/\/deb.debian.org\/debian/http:\/\/mirrors.aliyun.com\/debian/g' /etc/apt/sources.list && \
    sed -i 's/http:\/\/security.debian.org\/debian-security/http:\/\/mirrors.aliyun.com\/debian-security/g' /etc/apt/sources.list && \
    apt-get update && apt-get install -y --no-install-recommends ca-certificates && \
    update-ca-certificates
WORKDIR /metadata-manage
RUN touch backend.log && \
    chown 1000:1000 backend.log
ADD ./metadata-manage-web-*.jar ./metadata-manage-web.jar
ADD ./config ./config
CMD java -Dfile.encoding=UTF-8 $JAVA_OPTS -jar metadata-manage-web.jar  --spring.config.location=./config/