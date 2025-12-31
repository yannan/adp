package com.eisoo.metadatamanage.web.filter;

import com.eisoo.standardization.common.exception.UnauthorizedException;
import com.eisoo.standardization.common.util.AiShuUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@WebFilter(filterName = "tokenFilter")
public class TokenFilter implements Filter {

    @Autowired
    HandlerExceptionResolver handlerExceptionResolver;

    @Value("${token.check.url}")
    String tokenCheckUrl;

    @Value("${token.check.enable:true}")
    Boolean tokenCheckEnable;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        String uri = request.getRequestURI();
        // 这里处理内部接口：不需要token;需要x-user
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        String xUser = httpServletRequest.getHeader("x-user");
        if (AiShuUtil.isNotEmpty(xUser) && uri.startsWith("/api/metadata-manage/v1/table_and_column")) {
            chain.doFilter(servletRequest, servletResponse);
            return;
        }
        // /api/standardization/v1的为 controller的接口，这么做是要排除swagger的接口。
        if (tokenCheckEnable && uri.startsWith("/api/metadata-manage/v1")) {
            if (AiShuUtil.checkTokenValid(tokenCheckUrl, AiShuUtil.getToken(servletRequest))) {
                chain.doFilter(servletRequest, servletResponse);
            } else {
                // token校验不通过，抛出http 401
                handlerExceptionResolver.resolveException((HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse, null, new UnauthorizedException());
            }
        } else {
            // 非controller接口直接放行，这里主要是swagger资源
            chain.doFilter(servletRequest, servletResponse);
        }
    }
}
