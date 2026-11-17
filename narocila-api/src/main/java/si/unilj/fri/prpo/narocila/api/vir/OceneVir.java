package si.unilj.fri.prpo.narocila.api.vir;

import java.time.ZoneOffset;
import java.time.OffsetDateTime;
import java.net.URI;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.enterprise.context.ApplicationScoped;

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
            throw new WebApplicationException("Naročila " + narociloId + " s statusom " + narocilo.getStatus() + " ni mogoče oceniti.", Response.Status.CONFLICT); // pozneje KonfliktException(...)
        }

        Ocena ocena = new Ocena()
        .narociloId(narociloId)
        .ocena(novaOcena.getOcena())
        .komentar(novaOcena.getKomentar())
        .ustvarjeno(OffsetDateTime.now(ZoneOffset.UTC));

        if (shramba.ocene().putIfAbsent(narociloId, ocena) != null) {
            throw new WebApplicationException("Naročilo " + narociloId + " je že ocenjeno.", Response.Status.CONFLICT); // pozneje KonfliktException(...)
        }

        URI lokacija = URI.create("/v1/narocila/" + narociloId + "/ocena");
        return Response.created(lokacija).entity(ocena).build();
    }
}