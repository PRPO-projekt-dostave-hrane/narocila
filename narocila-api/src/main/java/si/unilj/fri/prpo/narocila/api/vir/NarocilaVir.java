package si.unilj.fri.prpo.narocila.api.vir;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.time.ZoneOffset;
import java.time.OffsetDateTime;
import java.net.URI;
import java.util.UUID;
import java.util.Map;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.enterprise.context.ApplicationScoped;

import si.unilj.fri.prpo.narocila.api.napake.KonfliktException;
import si.unilj.fri.prpo.narocila.api.napake.VnosException;
import si.unilj.fri.prpo.narocila.api.model.NovaPostavka;
import si.unilj.fri.prpo.narocila.api.model.Postavka;
import si.unilj.fri.prpo.narocila.api.model.SeznamNarocil;
import si.unilj.fri.prpo.narocila.api.model.StatusNarocila;
import si.unilj.fri.prpo.narocila.api.model.NovoNarocilo;
import si.unilj.fri.prpo.narocila.api.model.Narocilo;
import si.unilj.fri.prpo.narocila.api.NarocilaApi;

@ApplicationScoped
public class NarocilaVir implements NarocilaApi {
    private final NarocilaShramba shramba;

    public NarocilaVir(NarocilaShramba shramba) {
        this.shramba = shramba;
    }

    private Narocilo najdiNarocilo(String narociloId) {
        Narocilo narocilo = shramba.narocila().get(narociloId);
        if (narocilo == null) {
            throw new NotFoundException("Naročilo ni bilo najdeno.");
        }
        return narocilo;
    }

    private Response prehod(String narociloId, StatusNarocila novi, String dejanje, StatusNarocila... dovoljeni) {
        Narocilo narocilo = najdiNarocilo(narociloId);
        synchronized (narocilo) {
            if (!List.of(dovoljeni).contains(narocilo.getStatus())) {
                throw new KonfliktException("slab-prehod","Naročila " + narociloId + " s statusom " + narocilo.getStatus() + " ni mogoče " + dejanje + ".");
            }
            narocilo.setStatus(novi);
            return Response.ok(narocilo).build();
        }
    }

    @Override 
    public Response ustvariNarocilo(NovoNarocilo novoNarocilo) {
        Map<String, NarocilaShramba.JedIzKataloga> meni = shramba.meniji().get(novoNarocilo.getRestavracijaId());
        if (meni == null) {
            throw new VnosException("#/restavracijaId", "Restavracija " + novoNarocilo.getRestavracijaId() + " ne obstaja.");
        }

        List<Postavka> postavke = new ArrayList<>();
        for (int i = 0; i < novoNarocilo.getPostavke().size(); i++) {
            NovaPostavka nova = novoNarocilo.getPostavke().get(i);
            NarocilaShramba.JedIzKataloga jed = meni.get(nova.getJedId());
            if (jed == null) {
                throw new VnosException("#/postavke/" + i + "/jedId", "Jed " + nova.getJedId() + " ne obstaja.");
            }
            if (!jed.razpolozljiva()) {
                throw new KonfliktException("jed-ni-na-voljo", "Jed " + nova.getJedId() + " ni na voljo.");
            }
            postavke.add(new Postavka()
            .jedId(nova.getJedId())
            .ime(jed.ime())
            .cena(NarocilaShramba.getCena(jed.znesek()))
            .kolicina(nova.getKolicina()));
        }

        String id = "n-" + UUID.randomUUID();
        Narocilo narocilo = new Narocilo()
        .id(id)
        .restavracijaId(novoNarocilo.getRestavracijaId())
        .status(StatusNarocila.CAKA_NA_SPREJEM)
        .postavke(postavke)
        .skupaj(NarocilaShramba.skupaj(postavke))
        .naslovDostave(novoNarocilo.getNaslovDostave())
        .ustvarjeno(OffsetDateTime.now(ZoneOffset.UTC));

        shramba.narocila().put(id, narocilo);
        URI lokacija = URI.create("/v1/narocila/" + id);
        return Response.created(lokacija).entity(narocilo).build();
    }
    
    @Override 
    public Response seznamNarocil(StatusNarocila status, String restavracijaId, Integer limit, Integer offset) {
        List<Narocilo> seznam = shramba.narocila().values().stream()
        .filter(n -> status == null || n.getStatus().equals(status))
        .filter(n -> restavracijaId == null || n.getRestavracijaId().equals(restavracijaId))
        .sorted(Comparator.comparing(n -> n.getId()))
        .skip(offset)
        .limit(limit)
        .toList();

        return Response.ok(new SeznamNarocil().items(seznam)).build();
    }


    @Override 
    public Response podrobnostiNarocila(String narociloId) {
        Narocilo narocilo = najdiNarocilo(narociloId);
        return Response.ok(narocilo).build();
    }

    @Override 
    public Response prekliciNarocilo(String narociloId) {
        return prehod(narociloId, StatusNarocila.PREKLICANO, "preklicati", StatusNarocila.CAKA_NA_SPREJEM, StatusNarocila.SPREJETO);
    }

    @Override 
    public Response zavrniNarocilo(String narociloId) {
        return prehod(narociloId, StatusNarocila.ZAVRNJENO, "zavrniti", StatusNarocila.CAKA_NA_SPREJEM);
    }

    @Override 
    public Response sprejmiNarocilo(String narociloId) {
        return prehod(narociloId, StatusNarocila.SPREJETO, "sprejeti", StatusNarocila.CAKA_NA_SPREJEM);
    }

    @Override
    public Response placajNarocilo(UUID idempotencyKey, String narociloId) {
        Narocilo narocilo = najdiNarocilo(narociloId);
        synchronized(narocilo) {
            String prejsnjeNarocilo = shramba.placilniKljuci().get(idempotencyKey);
            if (prejsnjeNarocilo != null) {
                if (prejsnjeNarocilo.equals(narociloId)) {
                    return Response.ok(narocilo).build();
                }
                else {
                    throw new BadRequestException("Idempotency-Key je že uporabljen za drugo naročilo.");
                }
            }

            if (narocilo.getStatus() != StatusNarocila.SPREJETO) {
                throw new KonfliktException("slab-prehod", "Naročila " + narociloId + " s statusom " + narocilo.getStatus() + " ni mogoče plačati.");
            }

            String zeRezervirano = shramba.placilniKljuci().putIfAbsent(idempotencyKey, narociloId);
            if (zeRezervirano != null) {
                throw new BadRequestException("Idempotency-Key je že uporabljen za drugo naročilo.");
            }

            String transakcijaId = "tx-" + UUID.randomUUID();
            narocilo.setTransakcijaId(transakcijaId);
            narocilo.setStatus(StatusNarocila.PLACANO);
            return Response.ok(narocilo).build();
        }
    }

    @Override
    public Response prevzemiNarocilo(String narociloId) {
        return prehod(narociloId, StatusNarocila.V_DOSTAVI, "prevzeti", StatusNarocila.PLACANO);
    }

    @Override
    public Response dostaviNarocilo(String narociloId) {
        return prehod(narociloId, StatusNarocila.DOSTAVLJENO, "dostaviti", StatusNarocila.V_DOSTAVI);
    }
}