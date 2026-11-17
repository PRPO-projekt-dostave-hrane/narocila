package si.unilj.fri.prpo.narocila.api.vir;

import java.util.UUID;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;

import si.unilj.fri.prpo.narocila.api.model.Cena;
import si.unilj.fri.prpo.narocila.api.model.Narocilo;
import si.unilj.fri.prpo.narocila.api.model.Ocena;
import si.unilj.fri.prpo.narocila.api.model.Postavka;
import si.unilj.fri.prpo.narocila.api.model.StatusNarocila;

@ApplicationScoped
public class NarocilaShramba {

    private final Map<String, Narocilo> narocila = new ConcurrentHashMap<>();
    private final Map<String, Ocena> ocene = new ConcurrentHashMap<>();
    private final Map<UUID, String> placilniKljuci = new ConcurrentHashMap<>();

    public record JedIzKataloga(String ime, String znesek, boolean razpolozljiva) {}

    public NarocilaShramba() {
        dodaj("n-1", StatusNarocila.CAKA_NA_SPREJEM, null, "2026-11-14T18:00:00Z",
                margherita(2));
        dodaj("n-2", StatusNarocila.SPREJETO, null, "2026-11-14T18:10:00Z",
                margherita(1), tiramisu(1));
        dodaj("n-3", StatusNarocila.PLACANO, "tx-0003", "2026-11-14T18:20:00Z",
                tiramisu(2));
        dodaj("n-4", StatusNarocila.V_DOSTAVI, "tx-0004", "2026-11-14T18:30:00Z",
                margherita(3));
        dodaj("n-5", StatusNarocila.DOSTAVLJENO, "tx-0005", "2026-11-13T19:00:00Z",
                margherita(1));
        dodaj("n-6", StatusNarocila.DOSTAVLJENO, "tx-0006", "2026-11-12T19:00:00Z",
                margherita(2), tiramisu(1));
        dodaj("n-7", StatusNarocila.PREKLICANO, null, "2026-11-11T12:00:00Z",
                tiramisu(1));
        dodaj("n-8", StatusNarocila.ZAVRNJENO, null, "2026-11-11T13:00:00Z",
                margherita(1));

        ocene.put("n-6", new Ocena()
                .narociloId("n-6")
                .ocena(5)
                .komentar("Odlično, še topla.")
                .ustvarjeno(OffsetDateTime.parse("2026-11-12T19:45:00Z")));
    }

    private final Map<String, Map<String, JedIzKataloga>> meniji = Map.of(
            "r-1", Map.of(
                    "j-1", new JedIzKataloga("Margherita", "9.50", true),
                    "j-2", new JedIzKataloga("Tiramisu", "5.00", true),
                    "j-3", new JedIzKataloga("Calzone", "11.00", false)),
            "r-2", Map.of(),
            "r-3", Map.of());


    public Map<String, Narocilo> narocila() {
        return narocila;
    }

    public Map<String, Ocena> ocene() {
        return ocene;
    }

    public Map<UUID, String> placilniKljuci() {
        return placilniKljuci;
    }

    public Map<String, Map<String, JedIzKataloga>> meniji() {
        return meniji;
    }

    public static Cena getCena(String znesek) {
        return cena(znesek);
    }

    /** Skupni znesek: vsota cena × količina vseh postavk. */
    public static Cena skupaj(List<Postavka> postavke) {
        BigDecimal vsota = postavke.stream()
                .map(p -> new BigDecimal(p.getCena().getZnesek())
                        .multiply(BigDecimal.valueOf(p.getKolicina())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return cena(vsota.setScale(2).toPlainString());
    }

    private void dodaj(String id, StatusNarocila status, String transakcijaId,
                       String ustvarjeno, Postavka... postavke) {
        List<Postavka> seznam = new ArrayList<>(List.of(postavke));
        narocila.put(id, new Narocilo()
                .id(id)
                .restavracijaId("r-1")
                .status(status)
                .postavke(seznam)
                .skupaj(skupaj(seznam))
                .naslovDostave("Večna pot 113, Ljubljana")
                .ustvarjeno(OffsetDateTime.parse(ustvarjeno))
                .transakcijaId(transakcijaId));
    }

    private static Postavka margherita(int kolicina) {
        return new Postavka().jedId("j-1").ime("Margherita").cena(cena("9.50")).kolicina(kolicina);
    }

    private static Postavka tiramisu(int kolicina) {
        return new Postavka().jedId("j-2").ime("Tiramisu").cena(cena("5.00")).kolicina(kolicina);
    }

    private static Cena cena(String znesek) {
        return new Cena().znesek(znesek).valuta(Cena.ValutaEnum.EUR);
    }
}