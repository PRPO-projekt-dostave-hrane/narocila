package si.unilj.fri.prpo.narocila.api.vir;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static si.unilj.fri.prpo.narocila.api.vir.Pomoc.NAROCILA;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class NarocilaVirTest {

    // GET /v1/narocila

    @Test
    void seznamNarocil() {
        given()
                .when().get(NAROCILA)
                .then().statusCode(200)
                .body("items", not(empty()));
    }

    @Test
    void filterPoStatusu() {
        given().queryParam("status", "PREKLICANO")
                .when().get(NAROCILA)
                .then().statusCode(200)
                .body("items", not(empty()))
                .body("items.status", everyItem(equalTo("PREKLICANO")));
    }

    @Test
    void prevelikLimitVrne400() {
        given().queryParam("limit", 500)
                .when().get(NAROCILA)
                .then().statusCode(400)
                .contentType("application/problem+json")
                .body("errors[0].pointer", equalTo("limit"));
    }

    // GET /v1/narocila/{narociloId}

    @Test
    void podrobnostiNarocila() {
        given()
                .when().get(NAROCILA + "/n-7")
                .then().statusCode(200)
                .body("id", equalTo("n-7"))
                .body("status", equalTo("PREKLICANO"));
    }

    @Test
    void neobstojeceNarociloVrne404() {
        given()
                .when().get(NAROCILA + "/n-99")
                .then().statusCode(404)
                .contentType("application/problem+json")
                .body("type", endsWith("ni-najden"));
    }

    // POST /v1/narocila

    @Test
    void oddajaVrne201InLocation() {
        given().contentType(ContentType.JSON).body(Pomoc.NOVO_NAROCILO)
                .when().post(NAROCILA)
                .then().statusCode(201)
                .header("Location", containsString(NAROCILA + "/n-"))
                .body("status", equalTo("CAKA_NA_SPREJEM"))
                .body("postavke[0].ime", equalTo("Margherita"))
                .body("postavke[0].cena.znesek", equalTo("9.50"))
                .body("skupaj.znesek", equalTo("19.00"));
    }

    @Test
    void neveljavenJsonVrne400() {
        given().contentType(ContentType.JSON).body("{ to ni json")
                .when().post(NAROCILA)
                .then().statusCode(400)
                .contentType("application/problem+json");
    }

    @Test
    void neobstojecaRestavracijaVrne422() {
        given().contentType(ContentType.JSON)
                .body("""
                        {"restavracijaId": "r-99",
                         "postavke": [{"jedId": "j-1", "kolicina": 1}],
                         "naslovDostave": "Večna pot 113"}
                        """)
                .when().post(NAROCILA)
                .then().statusCode(422)
                .body("type", endsWith("vnos"))
                .body("errors[0].pointer", equalTo("#/restavracijaId"));
    }

    @Test
    void neobstojecaJedVrne422() {
        given().contentType(ContentType.JSON)
                .body("""
                        {"restavracijaId": "r-1",
                         "postavke": [{"jedId": "j-99", "kolicina": 1}],
                         "naslovDostave": "Večna pot 113"}
                        """)
                .when().post(NAROCILA)
                .then().statusCode(422)
                .body("errors[0].pointer", equalTo("#/postavke/0/jedId"));
    }

    @Test
    void neveljavnaKolicinaVrne422() {
        given().contentType(ContentType.JSON)
                .body("""
                        {"restavracijaId": "r-1",
                         "postavke": [{"jedId": "j-1", "kolicina": 0}],
                         "naslovDostave": "Večna pot 113"}
                        """)
                .when().post(NAROCILA)
                .then().statusCode(422)
                .body("errors[0].pointer", equalTo("#/postavke/0/kolicina"));
    }

    @Test
    void jedNiNaVoljoVrne409() {
        given().contentType(ContentType.JSON)
                .body("""
                        {"restavracijaId": "r-1",
                         "postavke": [{"jedId": "j-3", "kolicina": 1}],
                         "naslovDostave": "Večna pot 113"}
                        """)
                .when().post(NAROCILA)
                .then().statusCode(409)
                .body("type", endsWith("jed-ni-na-voljo"));
    }

    // Prehodi statusov

    @Test
    void celotenKrogDoDostave() {
        String id = Pomoc.ustvari();

        given().when().post(NAROCILA + "/" + id + "/sprejem")
                .then().statusCode(200).body("status", equalTo("SPREJETO"));

        given().header("Idempotency-Key", UUID.randomUUID().toString())
                .when().post(NAROCILA + "/" + id + "/placilo")
                .then().statusCode(200)
                .body("status", equalTo("PLACANO"))
                .body("transakcijaId", startsWith("tx-"));

        given().when().post(NAROCILA + "/" + id + "/prevzem")
                .then().statusCode(200).body("status", equalTo("V_DOSTAVI"));

        given().when().post(NAROCILA + "/" + id + "/dostava")
                .then().statusCode(200).body("status", equalTo("DOSTAVLJENO"));
    }

    @Test
    void zavrnitev() {
        String id = Pomoc.ustvari();
        given().when().post(NAROCILA + "/" + id + "/zavrnitev")
                .then().statusCode(200).body("status", equalTo("ZAVRNJENO"));
    }

    @Test
    void preklicPoSprejemu() {
        String id = Pomoc.sprejeto();
        given().when().post(NAROCILA + "/" + id + "/preklic")
                .then().statusCode(200).body("status", equalTo("PREKLICANO"));
    }

    @Test
    void prehodIzKoncnegaStanjaVrne409() {
        given().when().post(NAROCILA + "/n-7/sprejem")
                .then().statusCode(409)
                .contentType("application/problem+json")
                .body("type", endsWith("slab-prehod"));
    }

    @Test
    void dostavaPredPrevzemomVrne409() {
        String id = Pomoc.ustvari();
        given().when().post(NAROCILA + "/" + id + "/dostava")
                .then().statusCode(409)
                .body("type", endsWith("slab-prehod"));
    }

    @Test
    void prehodNeobstojecegaNarocilaVrne404() {
        given().when().post(NAROCILA + "/n-99/sprejem")
                .then().statusCode(404);
    }

    // POST /v1/narocila/{narociloId}/placilo

    @Test
    void ponovljenoPlaciloVrneIstiOdgovor() {
        String id = Pomoc.sprejeto();
        String kljuc = UUID.randomUUID().toString();

        String prvaTransakcija = given().header("Idempotency-Key", kljuc)
                .when().post(NAROCILA + "/" + id + "/placilo")
                .then().statusCode(200)
                .extract().path("transakcijaId");

        given().header("Idempotency-Key", kljuc)
                .when().post(NAROCILA + "/" + id + "/placilo")
                .then().statusCode(200)
                .body("transakcijaId", equalTo(prvaTransakcija));
    }

    @Test
    void istiKljucZaDrugoNarociloVrne400() {
        String prvo = Pomoc.sprejeto();
        String drugo = Pomoc.sprejeto();
        String kljuc = UUID.randomUUID().toString();

        given().header("Idempotency-Key", kljuc)
                .when().post(NAROCILA + "/" + prvo + "/placilo")
                .then().statusCode(200);

        given().header("Idempotency-Key", kljuc)
                .when().post(NAROCILA + "/" + drugo + "/placilo")
                .then().statusCode(400);
    }

    @Test
    void placiloBrezKljucaVrne400() {
        String id = Pomoc.sprejeto();
        given()
                .when().post(NAROCILA + "/" + id + "/placilo")
                .then().statusCode(400);
    }

    @Test
    void neveljavenKljucVrne400() {
        String id = Pomoc.sprejeto();
        given().header("Idempotency-Key", "abc")
                .when().post(NAROCILA + "/" + id + "/placilo")
                .then().statusCode(400);
    }

    @Test
    void placiloNesprejetegaVrne409() {
        String id = Pomoc.ustvari();
        given().header("Idempotency-Key", UUID.randomUUID().toString())
                .when().post(NAROCILA + "/" + id + "/placilo")
                .then().statusCode(409)
                .body("type", endsWith("slab-prehod"));
    }

    @Test
    void placiloNeobstojecegaVrne404() {
        given().header("Idempotency-Key", UUID.randomUUID().toString())
                .when().post(NAROCILA + "/n-99/placilo")
                .then().statusCode(404);
    }
}
