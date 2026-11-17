package si.unilj.fri.prpo.narocila.api.napake;

import java.util.List;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import si.unilj.fri.prpo.narocila.api.model.NapakaVnosa;
import si.unilj.fri.prpo.narocila.api.model.ValidacijskiProblem;
import si.unilj.fri.prpo.narocila.api.model.Problem;

@Provider
public class HttpNapakaMapper implements ExceptionMapper<WebApplicationException> {
    static final String OSNOVA = "https://dostava-hrane.example/problemi/";

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(WebApplicationException e) {
        String instance = uriInfo.getRequestUri().getPath();

        if (e instanceof VnosException v) {
            ValidacijskiProblem problem = new ValidacijskiProblem()
            .type(OSNOVA + "vnos")
            .title("Neveljavni podatki")
            .status(422)
            .detail(v.getMessage())
            .instance(instance)
            .errors(List.of(new NapakaVnosa().pointer(v.pointer()).detail(v.getMessage())));

            return Response.status(422)
            .type("application/problem+json")
            .entity(problem)
            .build();
        }

        Response.StatusType statusInfo = e.getResponse().getStatusInfo();
        int status = statusInfo.getStatusCode();

        String type = (e instanceof KonfliktException k) ? OSNOVA + k.tip() : tip(status);

        Problem problem = new Problem()
        .type(type)
        .title(naslov(status, statusInfo))
        .status(status)
        .detail(e.getMessage())
        .instance(instance);
        
        return Response.status(status)
        .type("application/problem+json")
        .entity(problem)
        .build();
    }

    private static String tip(int status) {
        return switch (status) {
            case 400 -> OSNOVA + "zahteva";
            case 404 -> OSNOVA + "ni-najden";
            default -> "about:blank";
        };
    }

    private static String naslov(int status, Response.StatusType statusInfo) {
        return switch (status) {
            case 400 -> "Napačna zahteva";
            case 404 -> "Vir ne obstaja";
            case 409 -> "Konflikt s trenutnim stanjem";
            default -> statusInfo.getReasonPhrase();
        };
    }
}