package si.unilj.fri.prpo.narocila.api.napake;

import jakarta.ws.rs.WebApplicationException;

public class VnosException extends WebApplicationException {

    private final String pointer;

    public VnosException(String pointer, String sporocilo) {
        super(sporocilo, 422);
        this.pointer = pointer;
    }

    public String pointer() {
        return pointer;
    }
}