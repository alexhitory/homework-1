package ru.homework.petstore;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.restassured.module.jsv.JsonSchemaValidator;

import java.io.IOException;
import java.io.InputStream;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchema;

final class PetstoreContract {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final JsonNode SWAGGER = loadSwagger();

    private PetstoreContract() {
    }

    static JsonSchemaValidator pet() {
        return definition("Pet");
    }

    static JsonSchemaValidator error() {
        return definition("ApiResponse");
    }

    private static JsonSchemaValidator definition(String name) {
        ObjectNode schema = MAPPER.createObjectNode();
        schema.put("$schema", "http://json-schema.org/draft-04/schema#");
        schema.put("$ref", "#/definitions/" + name);
        schema.set("definitions", SWAGGER.required("definitions"));
        return matchesJsonSchema(schema.toString());
    }

    private static JsonNode loadSwagger() {
        try (InputStream input = PetstoreContract.class.getResourceAsStream("/openapi/petstore-swagger.json")) {
            if (input == null) {
                throw new IllegalStateException("Не найден файл спецификации Swagger Petstore");
            }
            return MAPPER.readTree(input);
        } catch (IOException exception) {
            throw new IllegalStateException("Не удалось прочитать спецификацию Swagger Petstore", exception);
        }
    }
}
