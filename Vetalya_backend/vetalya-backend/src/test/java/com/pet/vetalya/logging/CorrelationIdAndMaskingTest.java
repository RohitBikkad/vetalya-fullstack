package com.pet.vetalya.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdAndMaskingTest {

    private CorrelationIdFilter filter;
    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
        logger = (Logger) LoggerFactory.getLogger(CorrelationIdAndMaskingTest.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
        logger.detachAppender(listAppender);
    }

    @Test
    @DisplayName("Valid incoming X-Request-Id is preserved in response and MDC")
    void validCorrelationIdIsPreserved() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "client-req-12345");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, (req, res) -> {
            assertThat(MDC.get("correlationId")).isEqualTo("client-req-12345");
            assertThat(MDC.get("traceId")).isEqualTo("client-req-12345");
            chain.doFilter(req, res);
        });

        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).isEqualTo("client-req-12345");
        // MDC must be cleared after request finishes
        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    @DisplayName("Invalid X-Request-Id (potential CRLF log injection) is sanitized and regenerated")
    void invalidCorrelationIdIsReplaced() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        // Attack attempt containing CRLF newline injection
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "malicious\r\nINJECTED_LOG_LINE");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            String correlationId = MDC.get("correlationId");
            assertThat(correlationId).isNotNull();
            assertThat(correlationId).doesNotContain("\r", "\n", "malicious");
        });

        String responseHeader = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(responseHeader).isNotNull();
        assertThat(responseHeader).doesNotContain("\r", "\n");
    }

    @Test
    @DisplayName("MaskingUtil masks phone numbers and email addresses accurately")
    void maskingUtilTests() {
        assertThat(MaskingUtil.maskPhone("+919876543210")).isEqualTo("+91XXXXXX3210");
        assertThat(MaskingUtil.maskPhone("9876543210")).isEqualTo("98XXXX3210");
        assertThat(MaskingUtil.maskPhone("1234")).isEqualTo("1234");

        assertThat(MaskingUtil.maskEmail("rajesh.kumar@gmail.com")).isEqualTo("r***@g***.com");
        assertThat(MaskingUtil.maskEmail("admin@vetalya.internal")).isEqualTo("a***@v***.internal");
    }

    @Test
    @DisplayName("SensitiveDataMaskingConverter redacts tokens, OTPs, passwords, and secrets")
    void sensitiveDataMaskingConverterTests() {
        String rawJwt = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyMTIzIn0.signaturePart123";
        String bearer = "Bearer " + rawJwt;
        String otpMessage = "Login attempt with otp=554433 and phone=+919876543210";
        String passwordMessage = "User payload password=\"MySecretPassword123!\"";

        String maskedBearer = SensitiveDataMaskingConverter.mask(bearer);
        assertThat(maskedBearer).contains("Bearer [REDACTED_JWT]");
        assertThat(maskedBearer).doesNotContain(rawJwt);

        String maskedOtp = SensitiveDataMaskingConverter.mask(otpMessage);
        assertThat(maskedOtp).contains("otp=******");
        assertThat(maskedOtp).doesNotContain("554433");
        assertThat(maskedOtp).contains("+91XXXXXX3210");
        assertThat(maskedOtp).doesNotContain("+919876543210");

        String maskedPassword = SensitiveDataMaskingConverter.mask(passwordMessage);
        assertThat(maskedPassword).contains("password=[REDACTED]");
        assertThat(maskedPassword).doesNotContain("MySecretPassword123!");
    }

    @Test
    @DisplayName("Unit test proving tokens and OTPs never appear in log output")
    void proofThatTokensAndOtpsNeverAppearInLogOutput() {
        String sampleToken = "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ1c2VyXzEifQ.xyz987";
        String rawLogText = "User auth failed for phone=+919876543210 with otp=849201 and token=" + sampleToken;

        String sanitized = SensitiveDataMaskingConverter.mask(rawLogText);
        logger.setLevel(Level.INFO);
        logger.info(sanitized);

        assertThat(listAppender.list).isNotEmpty();
        String formattedMessage = listAppender.list.get(0).getFormattedMessage();

        assertThat(formattedMessage).doesNotContain(sampleToken);
        assertThat(formattedMessage).doesNotContain("849201");
        assertThat(formattedMessage).contains("otp=******");
        assertThat(formattedMessage).contains("[REDACTED_JWT]");
    }
}
