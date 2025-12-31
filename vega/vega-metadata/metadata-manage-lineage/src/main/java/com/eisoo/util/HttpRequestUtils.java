package com.eisoo.util;

import com.alibaba.fastjson.JSONObject;
import com.eisoo.dto.AnyDataBuilderParaDto;
import com.eisoo.dto.AnyDataVidParaDto;
import com.eisoo.entity.TableLineageEntity;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.io.*;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.*;

/**
 * @Author: Lan Tian
 * @Date: 2024/5/8 17:32
 * @Version:1.0
 */
@Slf4j
public class HttpRequestUtils {
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
            connManager.setMaxTotal(1000);  // 连接池最大连接数
            connManager.setDefaultMaxPerRoute(20);  // 每个路由最大连接数
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class HttpDeleteWithBody extends HttpEntityEnclosingRequestBase {
        public static final String METHOD_NAME = "DELETE";

        /**
         * 获取方法（必须重载）
         *
         * @return
         */
        @Override
        public String getMethod() {
            return METHOD_NAME;
        }

        public HttpDeleteWithBody(final String uri) {
            super();
            setURI(URI.create(uri));
        }

        public HttpDeleteWithBody(final URI uri) {
            super();
            setURI(uri);
        }

        public HttpDeleteWithBody() {
            super();
        }
    }

    public static CloseableHttpClient createHttpsClient(Integer timeOut) throws Exception {
        RequestConfig requestConfig = RequestConfig.custom().setConnectionRequestTimeout(timeOut).setConnectTimeout(timeOut).setSocketTimeout(timeOut).build();
        // 配置超时回调机制
        HttpRequestRetryHandler retryHandler = new HttpRequestRetryHandler() {
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
                // 如果请求是幂等的，就再次尝试
                return !(request instanceof HttpEntityEnclosingRequest);
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
            // 如果是重定向
            if (302 == respCode) {
                String locationUrl = response.getLastHeader("Location").getValue();
                return getResult(new HttpPost(locationUrl), timeOut, isStream);
            } else if (200 == respCode) {
                // 获得响应实体
                HttpEntity entity = response.getEntity();
                sb = new StringBuilder();
                // 如果是以流的形式获取
                if (isStream) {
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
                // 如果是以流的形式获取
                if (isStream) {
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
        } catch (ConnectionPoolTimeoutException e) {
            log.warn("从连接池获取连接超时!!!");
            e.printStackTrace();
        } catch (SocketTimeoutException e) {
            log.warn("响应超时!!!");
            e.printStackTrace();
        } catch (ConnectTimeoutException e) {
            HttpRequestUtils.log.warn("请求超时");
            e.printStackTrace();
        } catch (final ClientProtocolException e) {
            HttpRequestUtils.log.warn("http协议错误");
            e.printStackTrace();
        } catch (final UnsupportedEncodingException e) {
            HttpRequestUtils.log.warn("不支持的字符编码");
            e.printStackTrace();
        } catch (final UnsupportedOperationException e) {
            HttpRequestUtils.log.warn("不支持的请求操作");
            e.printStackTrace();
        } catch (final ParseException e) {
            HttpRequestUtils.log.warn("解析错误");
            e.printStackTrace();
        } catch (final IOException e) {
            e.printStackTrace();
            HttpRequestUtils.log.warn("IO错误");
        } catch (final Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (null != response) {
                try {
                    response.close();
                } catch (final IOException e) {
                    HttpRequestUtils.log.warn("关闭响应连接出错");
                }
            }

        }
        return null == sb ? HttpRequestUtils.RESULT : (sb.toString().trim().isEmpty() ? "-1" : sb.toString());
    }

    /**
     * Map转换成NameValuePair List集合
     *
     * @param params map
     * @return NameValuePair List集合
     */
    public static List<NameValuePair> covertParams2NVPS(final Map<String, Object> params) {
        final List<NameValuePair> paramList = new LinkedList<>();
        for (final Map.Entry<String, Object> entry : params.entrySet()) {
            paramList.add(new BasicNameValuePair(entry.getKey(), entry.getValue().toString()));
        }
        return paramList;
    }

    /**
     * post请求,支持SSL
     *
     * @param url      请求地址
     * @param headers  请求头信息
     * @param jsonBody 请求参数
     * @param timeOut  超时时间(毫秒):从连接池获取连接的时间,请求时间,响应时间
     * @param isStream 是否以流的方式获取响应信息
     * @return 响应信息
     */
    public static String httpPost(final String url, final Map<String, Object> headers, final String jsonBody, final Integer timeOut, final boolean isStream) throws UnsupportedEncodingException {
        final HttpPost httpPost = new HttpPost(url);

        if (null != headers) {
            for (final Map.Entry<String, Object> entry : headers.entrySet()) {
                httpPost.addHeader(entry.getKey(), entry.getValue().toString());
            }
        }
        final RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(30000).setSocketTimeout(30000).setConnectionRequestTimeout(30000).build();
        httpPost.setConfig(requestConfig);

        // 添加请求参数信息
        if (null != jsonBody) {

            httpPost.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));
//            httpPost.setEntity(new UrlEncodedFormEntity(covertParams2NVPS(params), ENCODING));
        }
        return HttpRequestUtils.getResult(httpPost, timeOut, isStream);
    }

    public static String sendHttpsPostJsonColumnInfo(final String url, final Map<String, Object> headers, final String jsonBody) throws UnsupportedEncodingException {
        return HttpRequestUtils.httpPost(url, headers, jsonBody, 6000, false);
    }

    public static String sendHttpsPosToAdLineageJson(final String url, final Map<String, Object> headers, final String jsonBody) throws UnsupportedEncodingException {
        return HttpRequestUtils.httpPost(url, headers, jsonBody, 6000, false);
    }

    public static String sendHttpsPosToAdGraphBuild(final String url, final Map<String, Object> headers, final String jsonBody) throws UnsupportedEncodingException {
        return HttpRequestUtils.httpPost(url, headers, jsonBody, 6000, false);
    }

    public static String sendHttpsPosFormDataToAdGraphBuild(final String url, final Map<String, Object> headers, MultipartEntityBuilder builder) throws UnsupportedEncodingException {
        final HttpPost httpPost = new HttpPost(url);
        // 构建form-data请求体
        HttpEntity entity = builder.build();
        httpPost.setEntity(entity);
        if (null != headers) {
            for (final Map.Entry<String, Object> entry : headers.entrySet()) {
                httpPost.addHeader(entry.getKey(), entry.getValue().toString());
            }
        }
        final RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(30000).setSocketTimeout(30000).setConnectionRequestTimeout(30000).build();
        httpPost.setConfig(requestConfig);
        return HttpRequestUtils.getResult(httpPost, 6000, false);
    }


    public static String sendHttpsPostJson(final String url, final Map<String, Object> headers, final String jsonBody) throws UnsupportedEncodingException {
        final String response = HttpRequestUtils.httpPost(url, headers, jsonBody, 6000, false);
        final JsonNode jsonNode = JsonUtils.toJsonNode(response);
        assert null != jsonNode;
        if (jsonNode.hasNonNull("res")) {
            return jsonNode.get("res").asText();
        }
        log.info("==============sendHttpsPostJson exec end ! response:{}=====================", response);
        return response;
    }

    /**
     * post请求,支持SSL
     *
     * @param url     请求地址
     * @param params  请求参数
     * @param timeOut 超时时间(毫秒):从连接池获取连接的时间,请求时间,响应时间
     * @return 响应信息
     */
    public static String httpPost(final String url, final Map<String, Object> params, final Integer timeOut) throws Exception {
        final HttpPost httpPost = new HttpPost(url);
        if (null != params) {
            httpPost.setEntity(new UrlEncodedFormEntity(HttpRequestUtils.covertParams2NVPS(params), HttpRequestUtils.ENCODING));
        }
        return HttpRequestUtils.getResult(httpPost, timeOut, false);
    }

    /**
     * post请求,支持SSL
     *
     * @param url     请求地址
     * @param params  请求参数
     * @param timeOut 超时时间(毫秒):从连接池获取连接的时间,请求时间,响应时间
     * @return 响应信息
     */
    public static String httpPost(final String url, final JSONObject params, final Integer timeOut) throws UnsupportedEncodingException {
        final HttpPost httpPost = new HttpPost(url);
        if (null != params) {
            httpPost.setEntity(new UrlEncodedFormEntity(HttpRequestUtils.covertParams2NVPS(params), HttpRequestUtils.ENCODING));
        }
        return HttpRequestUtils.getResult(httpPost, timeOut, false);
    }

    /**
     * get请求,支持SSL
     *
     * @param url      请求地址
     * @param headers  请求头信息
     * @param params   请求参数
     * @param timeOut  超时时间(毫秒):从连接池获取连接的时间,请求时间,响应时间
     * @param isStream 是否以流的方式获取响应信息
     * @return 响应信息
     */
    public static String httpGet(final String url, final Map<String, Object> headers, final Map<String, Object> params, final Integer timeOut, final boolean isStream) throws URISyntaxException {
        final URIBuilder uriBuilder = new URIBuilder(url);
        // 添加请求参数信息
        if (null != params) {
            uriBuilder.setParameters(HttpRequestUtils.covertParams2NVPS(params));
        }
        final HttpGet httpGet = new HttpGet(url);
        if (null != headers) {
            for (final Map.Entry<String, Object> entry : headers.entrySet()) {
                httpGet.addHeader(entry.getKey(), entry.getValue().toString());
            }
        }
        return HttpRequestUtils.getResult(httpGet, timeOut, isStream);
    }

    /**
     * get请求,支持SSL
     *
     * @param url     请求地址
     * @param params  请求参数
     * @param timeOut 超时时间(毫秒):从连接池获取连接的时间,请求时间,响应时间
     * @return 响应信息
     */
    public static String httpGet(final String url, final Map<String, Object> params, final Integer timeOut) throws URISyntaxException {
        final URIBuilder uriBuilder = new URIBuilder(url);
        if (null != params) {
            uriBuilder.setParameters(HttpRequestUtils.covertParams2NVPS(params));
        }
        final HttpGet httpGet = new HttpGet(url);
        return HttpRequestUtils.getResult(httpGet, timeOut, false);
    }

    public static String httpGetRestApi(final String url, final Map<String, Object> headers) {
        HttpGet httpGet = new HttpGet(url);
        if (null != headers) {
            for (final Map.Entry<String, Object> entry : headers.entrySet()) {
                httpGet.addHeader(entry.getKey(), entry.getValue().toString());
            }
        }
        return HttpRequestUtils.getResult(httpGet, 6000, false);
    }

    public static String httpGet(final String url, final Map<String, Object> headers, final Map<String, Object> params) throws URISyntaxException {
        final URIBuilder uriBuilder = new URIBuilder(url);
        if (null != params) {
            uriBuilder.setParameters(HttpRequestUtils.covertParams2NVPS(params));
        }
        final HttpGet httpGet = new HttpGet(uriBuilder.build());
        if (null != headers) {
            for (final Map.Entry<String, Object> entry : headers.entrySet()) {
                httpGet.addHeader(entry.getKey(), entry.getValue().toString());
            }
        }
        return HttpRequestUtils.getResult(httpGet, 6000, false);
    }

    /**
     * get请求,支持SSL
     *
     * @param url      请求地址
     * @param headers  请求头信息
     * @param params   请求参数
     * @param timeOut  超时时间(毫秒):从连接池获取连接的时间,请求时间,响应时间
     * @param isStream 是否以流的方式获取响应信息
     * @return 响应信息
     */
    public static String httpGet(final String url, final JSONObject headers, final JSONObject params, final Integer timeOut, final boolean isStream) throws URISyntaxException {
        final URIBuilder uriBuilder = new URIBuilder(url);
        if (null != params) {
            uriBuilder.setParameters(HttpRequestUtils.covertParams2NVPS(params));
        }
        final HttpGet httpGet = new HttpGet(url);
        if (null != headers) {
            for (final Map.Entry<String, Object> entry : headers.entrySet()) {
                httpGet.addHeader(entry.getKey(), entry.getValue().toString());
            }
        }
        return HttpRequestUtils.getResult(httpGet, timeOut, isStream);
    }

    /**
     * get请求,支持SSL
     *
     * @param url     请求地址
     * @param params  请求参数
     * @param timeOut 超时时间(毫秒):从连接池获取连接的时间,请求时间,响应时间
     * @return 响应信息
     */
    public static String httpGet(final String url, final JSONObject params, final Integer timeOut) throws URISyntaxException {
        final URIBuilder uriBuilder = new URIBuilder(url);
        if (null != params) {
            uriBuilder.setParameters(HttpRequestUtils.covertParams2NVPS(params));
        }
        final HttpGet httpGet = new HttpGet(url);
        return HttpRequestUtils.getResult(httpGet, timeOut, false);
    }

    public static String httpGet(final String url, final Map params) throws URISyntaxException {
        final URIBuilder uriBuilder = new URIBuilder(url);
        if (null != params) {
            uriBuilder.setParameters(HttpRequestUtils.covertParams2NVPS(params));
        }
        final HttpGet httpGet = new HttpGet(url);
        return HttpRequestUtils.getResult(httpGet, 6000, false);
    }


    public static String httpDelete(final String url, final Map<String, Object> headers, final String jsonBody) throws Exception {
        final URIBuilder uriBuilder = new URIBuilder(url);
        HttpDeleteWithBody httpDelete = new HttpDeleteWithBody(url);
        if (null != headers) {
            for (final Map.Entry<String, Object> entry : headers.entrySet()) {
                httpDelete.addHeader(entry.getKey(), entry.getValue().toString());
            }
        }
        httpDelete.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));
        CloseableHttpClient httpClient = createHttpsClient(6000);
        CloseableHttpResponse response = httpClient.execute(httpDelete);
        HttpEntity entity = response.getEntity();
        String result = EntityUtils.toString(entity);
        if (200 == response.getStatusLine().getStatusCode()) {
            log.info("DELETE方式请求远程调用成功.msg={}", result);
        }
        try {
            response.close();
        } catch (final IOException e) {
            log.warn("关闭响应连接出错");
        }
        return result;
    }
}
