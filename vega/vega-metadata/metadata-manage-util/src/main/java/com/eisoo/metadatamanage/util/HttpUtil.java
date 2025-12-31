package com.eisoo.metadatamanage.util;

import com.eisoo.metadatamanage.util.constant.Constants;
import com.eisoo.metadatamanage.util.constant.Messages;
import com.eisoo.standardization.common.api.HttpResponseVo;
import com.eisoo.standardization.common.enums.ErrorCodeEnum;
import com.eisoo.standardization.common.exception.AiShuException;
import com.eisoo.standardization.common.helper.ARTraceHelper;
import com.eisoo.standardization.common.util.AiShuUtil;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceState;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Author: WangZiYu
 * @description:org.example.util
 * @Date: 2023/3/21 17:02
 */
@Slf4j
public class HttpUtil {
    private static final String DEFAULT_CHARSET = "UTF-8";
    private static PoolingHttpClientConnectionManager httpClientConnectionManager;

    /**
     * 执行Get请求
     *
     * @param url 接口路径
     * @return 接口返回值
     */
    public static String executeGet(String url, Map<String, String> headerMap) {
        String result = "";
        HttpGet httpGet = new HttpGet(url);
        //添加头部信息
        if (AiShuUtil.isNotEmpty(headerMap)) {
            for (Map.Entry<String, String> header : headerMap.entrySet()) {
                httpGet.addHeader(header.getKey(), header.getValue());
            }
        }
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        try {
            httpClient = HttpClientBuilder.create().build();
            HttpClientContext context = HttpClientContext.create();
            //response = httpClient.execute(httpGet, context);
            response = excute(httpGet, context);
            int state = response.getStatusLine().getStatusCode();
            if (state == HttpStatus.SC_OK) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    result = EntityUtils.toString(entity, DEFAULT_CHARSET);
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        } finally {
            try {
                response.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (httpClient != null)
                    httpClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * 执行Post请求
     *
     * @param url    请求路径
     * @param params 参数
     * @return 接口返回值
     */
    public static String executePost(String url, Map<String, String> params) {
        String reStr = "";
        HttpPost httpPost = new HttpPost(url);
        List<NameValuePair> paramsRe = new ArrayList<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            paramsRe.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }
        //CloseableHttpClient httpclient = HttpClientBuilder.create().build();
        CloseableHttpResponse response;
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(paramsRe));
            HttpClientContext context = HttpClientContext.create();
            //response = httpclient.execute(httpPost, context);
            response = excute(httpPost, context);
            HttpEntity entity = response.getEntity();
            reStr = EntityUtils.toString(entity, DEFAULT_CHARSET);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        } finally {
            httpPost.releaseConnection();
        }
        return reStr;
    }

    /**
     * 发送JSON格式body的POST请求
     *
     * @param url      地址
     * @param jsonBody json body
     * @return 接口返回值
     */
    public static String executePostWithJson(String url, String jsonBody, Map<String, String> headerMap) {
        String reStr = "";
        HttpPost httpPost = new HttpPost(url);
        //添加头部信息
        if (AiShuUtil.isNotEmpty(headerMap)) {
            for (Map.Entry<String, String> header : headerMap.entrySet()) {
                httpPost.addHeader(header.getKey(), header.getValue());
            }
        }
        //CloseableHttpClient httpclient = HttpClientBuilder.create().build();
        CloseableHttpResponse response;
        try {
            httpPost.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));
            HttpClientContext context = HttpClientContext.create();
            log.info("start executePostWithJson,jsonBody:{}", jsonBody);
            //response = httpclient.execute(httpPost, context);
            response = excute(httpPost, context);
            HttpEntity entity = response.getEntity();
            reStr = EntityUtils.toString(entity, DEFAULT_CHARSET);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage());
        } finally {
            httpPost.releaseConnection();
        }
        log.info("register task response:{}", reStr);
        return reStr;
    }

    public static HttpResponseVo executePost(String url, String jsonBody, Map<String, String> headerMap) {
        HttpPost httpPost = new HttpPost(url);
        //添加头部信息
        if (AiShuUtil.isNotEmpty(headerMap)) {
            for (Map.Entry<String, String> header : headerMap.entrySet()) {
                httpPost.addHeader(header.getKey(), header.getValue());
            }
        }
        HttpResponse response;
        int code = -1;
        try {
            httpPost.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));
            HttpClientContext context = HttpClientContext.create();
            log.info("start executePostWithJson,jsonBody:{}", jsonBody);
            //response = httpclient.execute(httpPost, context);
            response = excute(httpPost, context);
            HttpEntity httpEntity = response.getEntity();
            code = response.getStatusLine().getStatusCode();
            String result = EntityUtils.toString(httpEntity, DEFAULT_CHARSET);
            return new HttpResponseVo(code, result);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage());
        } finally {
            httpPost.releaseConnection();
        }
    }

    public static HttpResponseVo httpPostFromData(String url, Map<String, String> params, Map<String, String> headerMap) {
        HttpPost httpPost = new HttpPost(url);
        if (AiShuUtil.isNotEmpty(headerMap)) {
            for (Map.Entry<String, String> header : headerMap.entrySet()) {
                httpPost.addHeader(header.getKey(), header.getValue());
            }
        }
        httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded");

        //拼接参数体
        List<NameValuePair> paramList = new ArrayList<NameValuePair>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            paramList.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(paramList, "UTF-8"));
        } catch (Exception e) {
            throw new AiShuException(ErrorCodeEnum.InternalError, Messages.MESSAGE_UNSUPPORTED_ENCODING, e.toString());
        }

        return dopost(url, httpPost);
    }

    public static String executePutHttpRequest(String url, String requestBody, Map<String, String> headerMap) {
        String entityStr = null;
        CloseableHttpResponse response = null;
        //CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        try {
            HttpPut post = new HttpPut(url);
//            添加头部信息
            for (Map.Entry<String, String> header : headerMap.entrySet()) {
                post.addHeader(header.getKey(), header.getValue());
            }
            HttpEntity entity = new StringEntity(requestBody, "Utf-8");
            log.info("request body：" + requestBody);
            post.setEntity(entity);
            response = excute(post, null);
            // 获得响应的实体对象
            HttpEntity httpEntity = response.getEntity();
            // 使用Apache提供的工具类进行转换成字符串
            entityStr = EntityUtils.toString(httpEntity, "UTF-8");
            log.info("PUT请求路径：" + post);
            log.info("PUT请求结果：" + entityStr);
        } catch (ClientProtocolException e) {
            log.error("Http协议出现问题");
            e.printStackTrace();
        } catch (ParseException e) {
            log.error("解析错误");
            e.printStackTrace();
        } catch (IOException e) {
            log.error("IO异常");
            e.printStackTrace();
        }
        return entityStr;
    }

    public static String executeDeleteHttpRequest(String url, Map<String, String> headerMap) {
        String entityStr = null;
        CloseableHttpResponse response = null;
        //CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        try {
            HttpDelete post = new HttpDelete(url);
//            添加头部信息
            if (AiShuUtil.isNotEmpty(headerMap)) {
                for (Map.Entry<String, String> header : headerMap.entrySet()) {
                    post.addHeader(header.getKey(), header.getValue());
                }
            }
            response = excute(post, null);
            // 获得响应的实体对象
            HttpEntity httpEntity = response.getEntity();
            // 使用Apache提供的工具类进行转换成字符串
            entityStr = EntityUtils.toString(httpEntity, "UTF-8");
            log.info("请求路径：" + post);
            log.info("请求结果：" + entityStr);
        } catch (ClientProtocolException e) {
            log.error("Http协议出现问题");
            e.printStackTrace();
        } catch (ParseException e) {
            log.error("解析错误");
            e.printStackTrace();
        } catch (IOException e) {
            log.error("IO异常");
            e.printStackTrace();
        }
        return entityStr;
    }

    private static CloseableHttpResponse excute(HttpRequestBase request, HttpClientContext context) throws IOException {

        String builderName = request.getURI().toString();
        Span span = ARTraceHelper.spanStart(builderName,
                SpanKind.CLIENT,
                ARTraceHelper.spanContext2Context(ARTraceHelper.getSpanContextFromHttpRequestAttribute()));
        span.setAttribute("http.method", request.getMethod());
        span.setAttribute("http.route", builderName);
        span.setAttribute("http.client_ip", ARTraceHelper.getPodName());
        span.setAttribute("func.path", "");

        request.addHeader(ARTraceHelper.Keys.KEY_TRACE_PARENT, ARTraceHelper.W3CTraceContextPropagator.createTraceParent(span));
        TraceState traceState = span.getSpanContext().getTraceState();
        if (!traceState.isEmpty()) {
            request.addHeader(ARTraceHelper.Keys.KEY_TTRACE_STATE, ARTraceHelper.W3CTraceContextPropagator.createTraceParent(span));
        }

        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        CloseableHttpResponse response;
        try {
            if (null == context) {
                response = httpClient.execute(request);
            } else {
                response = httpClient.execute(request, context);
            }
            int state = response.getStatusLine().getStatusCode();
            if (state == HttpStatus.SC_OK) {
                ARTraceHelper.spanSuccessEnd(span);
            } else {
                ARTraceHelper.spanErrorEnd(span);
            }
            return response;
        } catch (Exception e) {
            ARTraceHelper.spanErrorEnd(span);
            throw e;
        }
    }


    public static HttpResponseVo httpPostFromPlain(String url, String text, Header[] headers, RequestConfig config) throws Exception {
        HttpPost post = new HttpPost(url);
        if (null != headers) {
            post.setHeaders(headers);
        }
        post.addHeader("Content-Type", "application/json;charset=" + DEFAULT_CHARSET);

        // 设置传输编码格式
        StringEntity stringEntity = new StringEntity(text, DEFAULT_CHARSET);
        post.setEntity(stringEntity);

        if (config != null) {
            post.setConfig(config);
        } else {
            config = RequestConfig.custom()
                    .setConnectTimeout(5000) // 连接超时：5000毫秒（5秒）
                    .setSocketTimeout(10000) // 读取超时：10000毫秒（10秒）
                    .build();
            post.setConfig(config);
        }
        return dopost(url, post);
    }

    private static HttpResponseVo dopost(String url, HttpUriRequest httpUriRequest) {

        String builderName = httpUriRequest.getURI().toString();
        Span span = ARTraceHelper.spanStart(builderName,
                SpanKind.CLIENT,
                ARTraceHelper.spanContext2Context(ARTraceHelper.getSpanContextFromHttpRequestAttribute()));
        span.setAttribute("http.method", httpUriRequest.getMethod());
        span.setAttribute("http.route", builderName);
        span.setAttribute("http.client_ip", ARTraceHelper.getPodName());
        span.setAttribute("func.path", "");

        ARTraceHelper.addMDC(span.getSpanContext());

        httpUriRequest.addHeader(ARTraceHelper.Keys.KEY_TRACE_PARENT, ARTraceHelper.W3CTraceContextPropagator.createTraceParent(span));
        TraceState traceState = span.getSpanContext().getTraceState();
        if (!traceState.isEmpty()) {
            httpUriRequest.addHeader(ARTraceHelper.Keys.KEY_TTRACE_STATE, ARTraceHelper.W3CTraceContextPropagator.createTraceParent(span));
        }

        HttpResponse httpresponse = null;
        int code = -1;
        try (CloseableHttpClient httpClient = declareHttpClientSSL(url)) {
            httpresponse = httpClient.execute(httpUriRequest);
            HttpEntity httpEntity = httpresponse.getEntity();
            code = httpresponse.getStatusLine().getStatusCode();
            String result = EntityUtils.toString(httpEntity, DEFAULT_CHARSET);
            return new HttpResponseVo(code, result);
        } catch (Exception e) {
            log.error(String.format("http请求失败，uri{%s},exception{%s}", new Object[]{url, e}));
        } finally {
            if (code == HttpStatus.SC_OK) {
                ARTraceHelper.spanSuccessEnd(span);
            } else {
                ARTraceHelper.spanErrorEnd(span);
            }
            ARTraceHelper.resetMDC(ARTraceHelper.getSpanContextFromHttpRequestAttribute());
        }
        return null;
    }

    private static CloseableHttpClient declareHttpClientSSL(String url) {
        if (url.startsWith("https://")) {
            return sslClient();
        } else {
            return HttpClientBuilder.create().setConnectionManager(httpClientConnectionManager).build();
        }
    }

    /**
     * 设置SSL请求处理
     */
    private static CloseableHttpClient sslClient() {
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            X509TrustManager tm = new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] xcs, String str) {
                }

                public void checkServerTrusted(X509Certificate[] xcs, String str) {
                }
            };
            ctx.init(null, new TrustManager[]{tm}, null);
            SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(ctx,
                    NoopHostnameVerifier.INSTANCE);
//			SSLConnectionSocketFactory sslConnectionSocketFactory = SSLConnectionSocketFactory.getSocketFactory();
            return HttpClients.custom().setSSLSocketFactory(sslConnectionSocketFactory).build();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

    public static HttpResponseVo httpGet(String url, Map<String, String> params, Header[] headers) throws Exception {
        StringBuffer param = new StringBuffer();
        if (params != null && !params.isEmpty()) {
            int i = 0;
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (i == 0) {
                    param.append("?");
                } else {
                    param.append("&");
                }
                param.append(entry.getKey()).append("=").append(entry.getValue());
                i++;
            }
        }
        url += param;
        HttpGet httpGet = new HttpGet(url);
        if (null != headers) {
            httpGet.setHeaders(headers);
        }
        httpGet.addHeader("Content-Type", "application/json;charset=" + DEFAULT_CHARSET);
        return dopost(url, httpGet);
    }
}
