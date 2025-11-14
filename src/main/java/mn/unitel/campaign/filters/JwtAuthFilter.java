package mn.unitel.campaign.filters;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import mn.unitel.campaign.CustomResponse;
import mn.unitel.campaign.services.JwtService;
import org.jboss.logging.Logger;

import java.util.Set;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class JwtAuthFilter implements ContainerRequestFilter {

    private static final Logger LOG = Logger.getLogger(JwtAuthFilter.class);

    private static final Set<String> PUBLIC_SEGMENTS = Set.of(

    );

    @Inject
    JwtService jwtService;

    @Inject
    RateLimiter rateLimiter;

    @Override
    public void filter(ContainerRequestContext ctx) {
        String rawPath = ctx.getUriInfo().getPath();
        String path = rawPath == null ? "" : rawPath.trim();
        LOG.debug("Incoming request path: " + path);

        String firstSegment = extractFirstSegment(path);
        boolean isPublic = PUBLIC_SEGMENTS.contains(firstSegment);

        if (isPublic)
            return;


        String authHeader = ctx.getHeaderString("Authorization");
        if (authHeader == null || !authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            abort(ctx, "Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring(7).trim();
        if (token.isEmpty()) {
            abort(ctx, "Empty token");
            return;
        }

        if (jwtService.isExpiredOrInvalid(token)) {
            abort(ctx, "Token expired or invalid");
            return;
        }

        String subject = jwtService.extractSubject(token);
        String phoneNo = jwtService.getStringClaim(token, "phoneNo");
        String tokiId = jwtService.getStringClaim(token, "tokiId");
        String nationalId = jwtService.getStringClaim(token, "nationalId");
        if (phoneNo == null) {
            abort(ctx, "Missing phoneNo claim");
            return;
        }

        // Optional: Rate limit spin endpoints
        if ("spin".equals(firstSegment)) {
            if (!rateLimiter.isAllowed(phoneNo)) {
                abort(ctx, "Too many spin requests. Please wait.");
                return;
            }
        }

        ctx.setProperty("jwt.tokiId", tokiId);
        ctx.setProperty("jwt.nationalId", nationalId);
        ctx.setProperty("jwt.subject", subject);
        ctx.setProperty("jwt.phoneNo", phoneNo);
    }

    private String extractFirstSegment(String path) {
        if (path.isEmpty()) return "";
        int slash = path.indexOf('/');
        return (slash == -1 ? path : path.substring(0, slash)).toLowerCase();
    }

    private void abort(ContainerRequestContext ctx, String message) {
        LOG.debug("Aborting request: " + message);
        ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                .entity(new CustomResponse<>("fail", message, null))
                .build());
    }
}