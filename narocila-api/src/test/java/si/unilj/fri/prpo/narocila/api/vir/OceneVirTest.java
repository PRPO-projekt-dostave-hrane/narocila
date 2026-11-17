package si.unilj.fri.prpo.narocila.api.vir;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static si.unilj.fri.prpo.narocila.api.vir.Pomoc.NAROCILA;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class OceneVirTest {

    private static final String OCENA = """
            {"ocena": 4, "komentar": "Hitro in toplo."}
            """;

    @Test
    void ocenaDostavljenegaVrne201() {
        String id = Pomoc.dostavljeno();

        given().contentType(ContentType.JSON).body(OCENA)
                .when().post(NAROCILA + "/" + id + "/ocena")
                .then().statusCode(201)
                .header("Location", endsWith(NAROCILA + "/" + id + "/ocena"))
                .body("narociloId", equalTo(id))
                .body("ocena", equalTo(4));
    }

    @Test
    void ponovnaOcenaVrne409() {
        String id = Pomoc.dostavljeno();

        given().contentType(ContentType.JSON).body(OCENA)
                .when().post(NAROCILA + "/" + id + "/ocena")
                .then().statusCode(201);

        given().contentType(ContentType.JSON).body(OCENA)
                .when().post(NAROCILA + "/" + id + "/ocena")
                .then().statusCode(409)
                .body("type", endsWith("ze-ocenjeno"));
    }

    @Test
    void ocenaNedostavljenegaVrne409() {
        String id = Pomoc.ustvari();

        given().contentType(ContentType.JSON).body(OCENA)
                .when().post(NAROCILA + "/" + id + "/ocena")
                .then().statusCode(409)
                .body("type", endsWith("ni-dostavljeno"));
    }

    @Test
    void ocenaNeobstojecegaVrne404() {
        given().contentType(ContentType.JSON).body(OCENA)
                .when().post(NAROCILA + "/n-99/ocena")
                .then().statusCode(404);
    }

    @Test
    void ocenaIzvenObmocjaVrne422() {
        String id = Pomoc.dostavljeno();

        given().contentType(ContentType.JSON).body("""
                        {"ocena": 7}
                        """)
                .when().post(NAROCILA + "/" + id + "/ocena")
                .then().statusCode(422)
                .body("errors[0].pointer", equalTo("#/ocena"));
    }

    @Test
    void neveljavenJsonVrne400() {
        given().contentType(ContentType.JSON).body("{ to ni json")
                .when().post(NAROCILA + "/n-5/ocena")
                .then().statusCode(400);
    }
}
