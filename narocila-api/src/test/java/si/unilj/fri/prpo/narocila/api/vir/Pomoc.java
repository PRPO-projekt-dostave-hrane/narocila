package si.unilj.fri.prpo.narocila.api.vir;

import static io.restassured.RestAssured.given;

import java.util.UUID;

import io.restassured.http.ContentType;

/**
 * Pomožne metode za teste: ustvarijo svežo naročilo in ga premaknejo v želeni status.
 * Testi tako ne spreminjajo mock podatkov, od katerih so odvisni drugi testi.
 */
final class Pomoc {

    static final String NAROCILA = "/v1/narocila";

    static final String NOVO_NAROCILO = """
            {"restavracijaId": "r-1",
             "postavke": [{"jedId": "j-1", "kolicina": 2}],
             "naslovDostave": "Večna pot 113, Ljubljana"}
            """;

    private Pomoc() {
    }

    /** Ustvari novo naročilo (CAKA_NA_SPREJEM) in vrne njegov ID. */
    static String ustvari() {
        return given().contentType(ContentType.JSON).body(NOVO_NAROCILO)
                .when().post(NAROCILA)
                .then().statusCode(201)
                .extract().path("id");
    }

    static void dejanje(String narociloId, String dejanje) {
        given().when().post(NAROCILA + "/" + narociloId + "/" + dejanje)
                .then().statusCode(200);
    }

    static void placaj(String narociloId) {
        given().header("Idempotency-Key", UUID.randomUUID().toString())
                .when().post(NAROCILA + "/" + narociloId + "/placilo")
                .then().statusCode(200);
    }

    /** Ustvari naročilo in ga sprejme (SPREJETO). */
    static String sprejeto() {
        String id = ustvari();
        dejanje(id, "sprejem");
        return id;
    }

    /** Ustvari naročilo in ga pripelje do DOSTAVLJENO. */
    static String dostavljeno() {
        String id = sprejeto();
        placaj(id);
        dejanje(id, "prevzem");
        dejanje(id, "dostava");
        return id;
    }
}
