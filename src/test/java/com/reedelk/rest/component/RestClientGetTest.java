package com.reedelk.rest.component;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.reedelk.runtime.api.commons.StringUtils;
import com.reedelk.runtime.api.message.Message;
import com.reedelk.runtime.api.message.MessageBuilder;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.reedelk.rest.internal.commons.HttpHeader.CONTENT_TYPE;
import static com.reedelk.rest.internal.commons.RestMethod.GET;
import static com.reedelk.runtime.api.message.content.MimeType.APPLICATION_JSON;
import static com.reedelk.runtime.api.message.content.MimeType.TEXT_PLAIN;

class RestClientGetTest extends RestClientAbstractTest {

    @Test
    void shouldGetExecuteCorrectlyWhenResponse200() {
        // Given
        String responseBody = "{\"Name\":\"John\"}";
        RestClient component = clientWith(GET, BASE_URL, PATH);

        WireMock.givenThat(get(urlEqualTo(PATH))
                .willReturn(aResponse()
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON.toString())
                        .withStatus(200)
                        .withBody(responseBody)));

        Message payload = MessageBuilder.get().empty().build();

        // Expect
        AssertHttpResponse
                .isSuccessful(component, payload, flowContext, responseBody, APPLICATION_JSON);
    }

    @Test
    void shouldGetThrowExceptionWhenResponseNot2xxAndNotEmptyBody() {
        // Given
        String expectedErrorMessage = "Error exception caused by XYZ";
        RestClient component = clientWith(GET, BASE_URL, PATH);

        givenThat(get(urlEqualTo(PATH))
                .willReturn(aResponse()
                        .withStatus(507)
                        .withHeader(CONTENT_TYPE, TEXT_PLAIN.toString())
                        .withBody(expectedErrorMessage)));

        Message emptyPayload = MessageBuilder.get().empty().build();

        // Expect
        AssertHttpResponse
                .isNotSuccessful(component, emptyPayload, flowContext, expectedErrorMessage);
    }

    @Test
    void shouldGetThrowExceptionWhenResponseNot2xxAndEmptyBody() {
        RestClient component = clientWith(GET, BASE_URL, PATH);

        givenThat(get(urlEqualTo(PATH))
                .willReturn(aResponse()
                        .withStatus(500)));

        Message emptyPayload = MessageBuilder.get().empty().build();

        // Expect
        AssertHttpResponse
                .isNotSuccessful(component, emptyPayload, flowContext, StringUtils.EMPTY);
    }
}
