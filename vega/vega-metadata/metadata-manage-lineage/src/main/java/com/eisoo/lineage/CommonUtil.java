package com.eisoo.lineage;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Map;

@Slf4j
public class CommonUtil {
    private static PoolingHttpClientConnectionManager connManager;
    private static final String ENCODING = "UTF-8";
    private static final String RESULT = "-1";
    static {
        try {
            // 创建ssl安全访问连接
            SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
                @Override
                public boolean isTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) {
                    return true;
                }
            }).build();
            Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create().register("http", PlainConnectionSocketFactory.INSTANCE).register("https", new SSLConnectionSocketFactory(sslContext)).build();
            connManager = new PoolingHttpClientConnectionManager(registry);
            connManager.setMaxTotal(10);
            connManager.setDefaultMaxPerRoute(20);  // 每个路由最大连接数
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private CommonUtil() {

    }

    public static CloseableHttpClient createHttpsClient(Integer timeOut) throws Exception {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(timeOut)
                .setConnectTimeout(timeOut)
                .setSocketTimeout(timeOut)
                .build();
        HttpRequestRetryHandler retryHandler = new HttpRequestRetryHandler() { // 配置超时回调机制
            public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
                // 如果已经重试了5次，就放弃
                if (executionCount >= 5) {
                    return false;
                }
                // 如果服务器丢掉了连接，那么就重试
                if (exception instanceof NoHttpResponseException) {
                    return true;
                }
                // 不要重试SSL握手异常
                if (exception instanceof SSLHandshakeException) {
                    return false;
                }
                // 超时
                if (exception instanceof InterruptedIOException) {
                    return true;
                }
                // 目标服务器不可达
                if (exception instanceof UnknownHostException) {
                    return false;
                }
                // ssl握手异常
                if (exception instanceof SSLException) {
                    return false;
                }
                HttpClientContext clientContext = HttpClientContext.adapt(context);
                HttpRequest request = clientContext.getRequest();
                return !(request instanceof HttpEntityEnclosingRequest);  // 如果请求是幂等的，就再次尝试
            }
        };
        return HttpClients.custom().setConnectionManager(connManager).setDefaultRequestConfig(requestConfig).setRetryHandler(retryHandler).build();
    }
    private static String getResult(HttpRequestBase httpRequest, Integer timeOut, boolean isStream) {
        StringBuilder sb = null;
        CloseableHttpResponse response = null;
        try {
            CloseableHttpClient httpClient = createHttpsClient(timeOut);
            response = httpClient.execute(httpRequest);
            int respCode = response.getStatusLine().getStatusCode();
            if (302 == respCode) { // 如果是重定向
                String locationUrl = response.getLastHeader("Location").getValue();
                return getResult(new HttpPost(locationUrl), timeOut, isStream);
            } else if (200 == respCode) {
                HttpEntity entity = response.getEntity(); // 获得响应实体
                sb = new StringBuilder();
                if (isStream) {// 如果是以流的形式获取
                    BufferedReader br = new BufferedReader(new InputStreamReader(entity.getContent(), ENCODING));
                    String len = "";
                    while ((len = br.readLine()) != null) {
                        sb.append(len);
                    }
                } else {
                    sb.append(EntityUtils.toString(entity, ENCODING));
                    if (sb.length() < 1) {
                        sb.append("-1");
                    }
                }
            } else {
                HttpEntity entity = response.getEntity();
                sb = new StringBuilder();
                if (isStream) {   // 如果是以流的形式获取
                    BufferedReader br = new BufferedReader(new InputStreamReader(entity.getContent(), ENCODING));
                    String len = "";
                    while ((len = br.readLine()) != null) {
                        sb.append(len);
                    }
                } else {
                    sb.append(EntityUtils.toString(entity, ENCODING));
                    if (sb.length() < 1) {
                        sb.append("-1");
                    }
                }
            }
        } catch (Exception e) {
            log.warn("从连接池获取连接超时!!!");
            e.printStackTrace();
        } finally {
            if (null != response) {
                try {
                    response.close();
                } catch (IOException e) {
                    log.warn("关闭响应连接出错");
                }
            }
        }
        return null == sb ? RESULT : (sb.toString().trim().isEmpty() ? "-1" : sb.toString());
    }

    public static String httpGetRestApi(final String url, final Map<String, Object> headers) {
        HttpGet httpGet = new HttpGet(url);
        if (null != headers) {
            for (final Map.Entry<String, Object> entry : headers.entrySet()) {
                httpGet.addHeader(entry.getKey(), entry.getValue().toString());
            }
        }
        return getResult(httpGet, 6000, false);
    }

    /**
     * 判断集合是否为空 coll->null->true coll-> coll.size() == 0 -> true
     */
    public static <T> boolean isEmpty(Collection<T> coll) {
        return (coll == null || coll.isEmpty());
    }

    /**
     * 判断集合是否不为空
     */
    public static <T> boolean isNotEmpty(Collection<T> coll) {
        return !isEmpty(coll);
    }

    /**
     * 判断map是否为空
     */
    public static <K, V> boolean isEmpty(Map<K, V> map) {
        return (map == null || map.isEmpty());
    }

    /**
     * 判断map是否不为空
     */
    public static <K, V> boolean isNotEmpty(Map<K, V> map) {
        return !isEmpty(map);
    }

    /***
     * 判断String是否不为空
     * @param str
     * @return
     */

    public static boolean isNotEmpty(String str) {
        if (str == null || str.length() == 0) {
            return false;
        }
        return true;
    }

    public static boolean isEmpty(String str) {
        return !isNotEmpty(str);
    }

    /**
     * 判断一个对象是否为空
     */
    public static <T> boolean isEmpty(T t) {
        if (t == null) {
            return true;
        }
        return isEmpty(t.toString());
    }

}
