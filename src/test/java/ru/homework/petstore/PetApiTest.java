package ru.homework.petstore;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@DisplayName("POST /pet и GET /pet/{petId}")
class PetApiTest {
    private final List<Long> createdIds = new ArrayList<>();
    private final RequestSpecification request = new RequestSpecBuilder()
            .setBaseUri(System.getProperty("baseUrl", "https://petstore.swagger.io/v2"))
            .setContentType(ContentType.JSON)
            .setAccept(ContentType.JSON)
            .setConfig(RestAssuredConfig.config().httpClient(HttpClientConfig.httpClientConfig()
                    .setParam("http.connection.timeout", 10_000)
                    .setParam("http.socket.timeout", 15_000)))
            .build();

    @ParameterizedTest(name = "POST: создание со статусом {0}")
    @ValueSource(strings = {"available", "pending", "sold"})
    void createsPetWithEachAllowedStatus(String status) {
        long id = newId();
        Map<String, Object> pet = Map.of(
                "id", id,
                "name", "Test pet " + id,
                "photoUrls", List.of("https://example.org/pet.png"),
                "category", Map.of("id", 12, "name", "dogs"),
                "tags", List.of(Map.of("id", 34, "name", "api-test")),
                "status", status);

        create(pet).then()
                .statusCode(200).contentType(ContentType.JSON)
                .body(PetstoreContract.pet())
                .body("id", equalTo(id))
                .body("name", equalTo(pet.get("name")))
                .body("status", equalTo(status))
                .body("photoUrls", equalTo(pet.get("photoUrls")))
                .body("category.id", equalTo(12))
                .body("category.name", equalTo("dogs"))
                .body("tags[0].id", equalTo(34))
                .body("tags[0].name", equalTo("api-test"));
    }

    @Test
    @DisplayName("POST: пустые name и photoUrls допустимы по схеме (нижняя граница)")
    void acceptsEmptyNameAndPhotoUrls() {
        long id = newId();
        create(Map.of("id", id, "name", "", "photoUrls", List.of()))
                .then().statusCode(200).contentType(ContentType.JSON)
                .body(PetstoreContract.pet())
                .body("id", equalTo(id))
                .body("name", equalTo(""))
                .body("photoUrls", empty());
    }

    @Test
    @DisplayName("POST: сохранение кириллицы и специальных символов в имени")
    void preservesUnicodeAndSpecialCharacters() {
        long id = newId();
        String name = "Кот Барсик — \"друг\" & <pet> 🐾";
        create(Map.of("id", id, "name", name, "photoUrls", List.of()))
                .then().statusCode(200).contentType(ContentType.JSON)
                .body(PetstoreContract.pet())
                .body("id", equalTo(id))
                .body("name", equalTo(name));
    }

    @Test
    @DisplayName("GET: получение собственного питомца после POST")
    void getsCreatedPetById() {
        long id = newId();
        String name = "Get test " + id;
        create(Map.of("id", id, "name", name, "photoUrls", List.of(), "status", "available"))
                .then().statusCode(200).body(PetstoreContract.pet());

        given().spec(request).log().ifValidationFails()
                .when().get("/pet/{petId}", id)
                .then().log().ifValidationFails()
                .statusCode(200).contentType(ContentType.JSON)
                .body(PetstoreContract.pet())
                .body("id", equalTo(id))
                .body("name", equalTo(name))
                .body("status", equalTo("available"))
                .body("photoUrls", empty());
    }

    @Test
    @DisplayName("GET: отсутствующий питомец возвращает 404")
    void returns404ForMissingPet() {
        given().spec(request).log().ifValidationFails()
                .when().get("/pet/{petId}", -newId())
                .then().log().ifValidationFails()
                .statusCode(404).contentType(ContentType.JSON)
                .body(PetstoreContract.error())
                .body("code", equalTo(1))
                .body("type", equalTo("error"))
                .body("message", equalTo("Pet not found"));
    }

    @ParameterizedTest(name = "GET: невалидный int64 ID = {0}")
    @ValueSource(strings = {"not-a-number", "9223372036854775808"})
    void rejectsInvalidIdAsCurrentlyImplemented(String id) {
        given().spec(request).log().ifValidationFails()
                .when().get("/pet/{petId}", id)
                .then().log().ifValidationFails()
                .statusCode(404).contentType(ContentType.JSON)
                .body(PetstoreContract.error())
                .body("code", equalTo(404))
                .body("message", containsString("NumberFormatException"));
    }

    @Test
    @DisplayName("POST: синтаксически некорректный JSON отклоняется с 400")
    void rejectsMalformedJson() {
        given().spec(request).body("{\"id\":").log().ifValidationFails()
                .when().post("/pet")
                .then().log().ifValidationFails()
                .statusCode(400).contentType(ContentType.JSON)
                .body(PetstoreContract.error())
                .body("code", equalTo(400))
                .body("message", equalTo("bad input"));
    }

    private Response create(Map<String, Object> pet) {
        createdIds.add((Long) pet.get("id"));
        return given().spec(request).body(pet).log().ifValidationFails()
                .when().post("/pet")
                .then().log().ifValidationFails().extract().response();
    }

    private static long newId() {
        return ThreadLocalRandom.current().nextLong(1_000_000_000_000L, 9_000_000_000_000_000L);
    }

    @AfterEach
    void deleteCreatedPets() {
        for (long id : createdIds) {
            given().spec(request)
                    .when().delete("/pet/{petId}", id)
                    .then().log().ifValidationFails()
                    .statusCode(anyOf(is(200), is(404)));
        }
    }
}
