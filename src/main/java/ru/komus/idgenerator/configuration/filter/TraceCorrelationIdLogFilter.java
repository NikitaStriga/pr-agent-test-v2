package ru.komus.idgenerator.configuration.filter;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.UUID;

/**
 * This filter trace correlation id from the header of request. Create correlation id by itself if header is empty.
 */
@Component
public class TraceCorrelationIdLogFilter implements Filter
{
    private static final String CORRELATION_ID_HEADER = "correlation-id";

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException
    {
        String header = ((HttpServletRequest) servletRequest).getHeader(CORRELATION_ID_HEADER);
        if (header == null)
        {
            header = UUID.randomUUID().toString();
        }
        MDC.put(CORRELATION_ID_HEADER, header);
        filterChain.doFilter(servletRequest, servletResponse);
        MDC.clear();
    }
}