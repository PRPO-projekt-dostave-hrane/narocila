package si.unilj.fri.prpo.narocila.api.vir;

import java.time.ZoneOffset;
import java.time.OffsetDateTime;
import java.net.URI;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.enterprise.context.ApplicationScoped;

import si.unilj.fri.prpo.narocila.api.napake.KonfliktException;
import si.unilj.fri.prpo.narocila.api.model.Narocilo;
import si.unilj.fri.prpo.narocila.api.model.StatusNarocila;
import si.unilj.fri.prpo.narocila.api.model.NovaOcena;
import si.unilj.fri.prpo.narocila.api.model.Ocena;
import si.unilj.fri.prpo.narocila.api.OceneApi;

@ApplicationScoped
public class OceneVir implements OceneApi {
    private final NarocilaShramba shramba;

    public OceneVir(NarocilaShramba shramba) {
        this.shramba = shramba;
    }

    @Override 
    public Response oceniNarocilo(String narociloId, NovaOcena novaOcena) {
        Narocilo narocilo = shramba.narocila().get(narociloId);
        if (narocilo == null) {
            throw new NotFoundException("Naročilo ni bilo najdeno.");
        }
        if (narocilo.getStatus() != StatusNarocila.DOSTAVLJENO) {
            throw new KonfliktException("ni-dostavljeno", "Naročila " + narociloId + " s statusom " + narocilo.getStatus() + " ni mogoče oceniti.");
        }

        Ocena ocena = new Ocena()
        .narociloId(narociloId)
        .ocena(novaOcena.getOcena())
        .komentar(novaOcena.getKomentar())
        .ustvarjeno(OffsetDateTime.now(ZoneOffset.UTC));

        if (shramba.ocene().putIfAbsent(narociloId, ocena) != null) {
            throw new KonfliktException("ze-ocenjeno", "Naročilo " + narociloId + " je že ocenjeno.");
        }

        URI lokacija = URI.create("/v1/narocila/" + narociloId + "/ocena");
        return Response.created(lokacija).entity(ocena).build();
    }
}