package si.unilj.fri.prpo.narocila.api.napake;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

public class KonfliktException extends WebApplicationException {

    private final String tip;

    public KonfliktException(String tip, String sporocilo) {
        super(sporocilo, Response.Status.CONFLICT);
        this.tip = tip;
    }

    public String tip() {
        return tip;
    }
}