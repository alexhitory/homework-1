package ru.homework.petstore;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;

@DisplayName("Локальная валидация модели Pet из официального Swagger")
class PetContractTest {
    @Test
    @DisplayName("Контракт допускает минимальный Pet и пустой массив photoUrls")
    void acceptsMinimalPet() {
        assertThat("{\"name\":\"cat\",\"photoUrls\":[]}", PetstoreContract.pet());
    }

    @ParameterizedTest(name = "Контракт отклоняет: {0}")
    @ValueSource(strings = {
            "{\"photoUrls\":[]}",
            "{\"name\":\"cat\"}",
            "{\"name\":\"cat\",\"photoUrls\":\"photo.png\"}",
            "{\"name\":\"cat\",\"photoUrls\":[],\"status\":\"unknown\"}",
            "{\"name\":\"cat\",\"photoUrls\":[],\"category\":{\"id\":\"abc\"}}"
    })
    void rejectsInvalidPet(String json) {
        assertThat(json, not(PetstoreContract.pet()));
    }
}
