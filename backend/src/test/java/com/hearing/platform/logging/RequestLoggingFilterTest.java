package com.hearing.platform.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestLoggingFilterTest {

    private final RequestLoggingFilter filter = new RequestLoggingFilter();
    private final Logger accessLogger = (Logger) LoggerFactory.getLogger("com.hearing.platform.access");
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void attachAppender() {
        MDC.clear();
        appender = new ListAppender<>();
        appender.start();
        accessLogger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        accessLogger.detachAppender(appender);
        appender.stop();
        MDC.clear();
    }

    @Test
    void logsSuccessfulApiRequestWithoutSensitiveRequestData() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/lessons");
        request.setQueryString("password=secret&token=secret-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {
            LoggingContext.setUser(7L, "TEACHER");
            ((HttpServletResponse) res).setStatus(200);
        };

        filter.doFilter(request, response, chain);

        assertThat(appender.list).hasSize(1);
        String message = appender.list.get(0).getFormattedMessage();
        assertThat(message)
                .contains("method=GET")
                .contains("path=/api/lessons")
                .contains("status=200")
                .contains("userId=7")
                .contains("role=TEACHER")
                .doesNotContain("secret")
                .doesNotContain("Authorization");
        assertThat(MDC.get(LoggingContext.REQUEST_ID)).isNull();
        assertThat(MDC.get(LoggingContext.USER_ID)).isNull();
    }

    @Test
    void logsClientErrorStatus() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/ai/prepare/9");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {
            LoggingContext.setUser(11L, "STUDENT");
            ((HttpServletResponse) res).setStatus(403);
        };

        filter.doFilter(request, response, chain);

        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.get(0).getFormattedMessage())
                .contains("method=POST")
                .contains("path=/api/ai/prepare/9")
                .contains("status=403")
                .contains("userId=11")
                .contains("role=STUDENT");
    }

    @Test
    void skipsNonApiAndOptionsRequests() throws Exception {
        FilterChain noOp = (req, res) -> {
        };

        filter.doFilter(new MockHttpServletRequest("GET", "/files/vtt/demo.vtt"),
                new MockHttpServletResponse(), noOp);
        filter.doFilter(new MockHttpServletRequest("OPTIONS", "/api/lessons"),
                new MockHttpServletResponse(), noOp);

        assertThat(appender.list).isEmpty();
    }

    @Test
    void marksServerErrorAndClearsMdcWhenRequestFails() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/lessons");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {
            LoggingContext.setUser(13L, "TEACHER");
            throw new ServletException("boom");
        };

        assertThatThrownBy(() -> filter.doFilter(request, response, chain))
                .isInstanceOf(ServletException.class);

        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.get(0).getFormattedMessage()).contains("status=500");
        assertThat(MDC.get(LoggingContext.REQUEST_ID)).isNull();
        assertThat(MDC.get(LoggingContext.USER_ID)).isNull();
    }
}
