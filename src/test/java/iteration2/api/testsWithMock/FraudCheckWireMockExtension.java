package iteration2.api.testsWithMock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.Fault;
import common.annotations.FraudCheckMock;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Locale;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class FraudCheckWireMockExtension
        implements BeforeEachCallback, AfterEachCallback {

    private WireMockServer wireMockServer;

    @Override
    public void beforeEach(ExtensionContext context) {
        FraudCheckMock mockConfig = context.getTestMethod()
                .map(method -> method.getAnnotation(FraudCheckMock.class))
                .orElseGet(() -> context.getTestClass()
                        .map(clazz -> clazz.getAnnotation(FraudCheckMock.class))
                        .orElse(null));

        if (mockConfig != null) {
            setupWireMock(mockConfig);
        }
    }

    private void setupWireMock(FraudCheckMock config) {
        wireMockServer = new WireMockServer(
                WireMockConfiguration.wireMockConfig()
                        .port(config.port()));

        wireMockServer.start();

        setupStub(config);
    }

    private void setupStub(FraudCheckMock config) {
        switch (config.behavior()) {
            case RESPONSE -> setupResponseStub(config);
            case TIMEOUT -> setupTimeoutStub(config);
            case CONNECTION_ERROR -> setupConnectionErrorStub(config);
            case HTTP_ERROR -> setupHttpErrorStub(config);
        }
    }

    private void setupResponseStub(FraudCheckMock config) {
        wireMockServer.stubFor(
                post(urlPathMatching(config.endpoint())).willReturn(aResponse().withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody(createResponseBody(config))));
    }

    private void setupTimeoutStub(FraudCheckMock config) {
        wireMockServer.stubFor(
                post(urlPathMatching(config.endpoint())).willReturn(aResponse().withStatus(200)
                                .withFixedDelay(config.delayMs())));
    }

    private void setupConnectionErrorStub(FraudCheckMock config) {
        wireMockServer.stubFor(
                post(urlPathMatching(config.endpoint())).willReturn(aResponse()
                        .withFault(Fault.CONNECTION_RESET_BY_PEER)));
    }

    private void setupHttpErrorStub(FraudCheckMock config) {
        wireMockServer.stubFor(
                post(urlPathMatching(config.endpoint()))
                        .willReturn(
                                aResponse()
                                        .withStatus(config.httpStatus())
                        )
        );
    }

    private String createResponseBody(FraudCheckMock config) {
        return String.format(
                Locale.US,
                """
                {
                  "status": "%s",
                  "decision": "%s",
                  "riskScore": %.1f,
                  "reason": "%s",
                  "requiresManualReview": %s,
                  "additionalVerificationRequired": %s
                }
                """,
                config.status(),
                config.decision(),
                config.riskScore(),
                config.reason(),
                config.requiresManualReview(),
                config.additionalVerificationRequired()
        );
    }

    @Override
    public void afterEach(ExtensionContext context) {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }

    public String getBaseUrl() {
        if (wireMockServer == null || !wireMockServer.isRunning()) {
            return null;
        }

        return "http://host.docker.internal:" + wireMockServer.port();
    }
}