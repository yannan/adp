package com.eisoo.metadatamanage.web.filter;

import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 参数前后空格处理
 */
@Component
@WebFilter(urlPatterns = "/**", filterName = "paramTrimFilter", dispatcherTypes = DispatcherType.REQUEST)
public class ParamTrimFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        ParameterRequestWrapper parmsRequest = new ParameterRequestWrapper((HttpServletRequest) request);
        chain.doFilter(parmsRequest, response);

    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
