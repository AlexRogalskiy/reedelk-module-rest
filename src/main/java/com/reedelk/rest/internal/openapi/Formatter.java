package com.reedelk.rest.internal.openapi;

import com.reedelk.openapi.OpenApiSerializer;
import com.reedelk.openapi.v3.OpenApiObject;
import com.reedelk.rest.component.listener.openapi.v3.OpenApiSerializableContext;
import com.reedelk.rest.internal.commons.HttpContentType;
import com.reedelk.runtime.api.message.content.MimeType;

public enum Formatter {

    JSON {
        @Override
        String format(OpenApiObject openApiObject, OpenApiSerializableContext context) {
            return OpenApiSerializer.toJson(openApiObject);
        }

        @Override
        String contentType() {
            return MimeType.APPLICATION_JSON.toString();
        }
    },

    YAML {
        @Override
        String format(OpenApiObject openApiObject, OpenApiSerializableContext context) {
            return OpenApiSerializer.toYaml(openApiObject);
        }

        @Override
        String contentType() {
            return HttpContentType.YAML_CONTENT_TYPE;
        }
    };

    abstract String format(OpenApiObject openApiObject, OpenApiSerializableContext context);

    abstract String contentType();
}
