package mn.unitel.campaign.services;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;
import java.util.Map;

@ApplicationScoped
public class JwtService {
    @ConfigProperty(name = "jwt.key")
    String SECRET_KEY;

    public String generateTokenWithPhone(String subject, String phoneNo, String tokiId, String nationalId) {
        Map<String, Object> claims = Map.of(
                "phoneNo", phoneNo,
                "tokiId", tokiId,
                "nationalId", nationalId);

        return generateToken(subject, claims);
    }

    private String generateToken(String subject, Map<String, Object> claims) {
        long nowMillis = System.currentTimeMillis();

        long expMillis = nowMillis + 15 * 60 * 1000; // 10 min
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(nowMillis))
                .setExpiration(new Date(expMillis))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY.getBytes())
                .compact();
    }

    public boolean isExpiredOrInvalid(String token) {
        try {
            Date exp = Jwts.parser()
                    .setSigningKey(SECRET_KEY.getBytes())
                    .parseClaimsJws(token)
                    .getBody()
                    .getExpiration();
            return exp.before(new Date());
        } catch (Exception e) {
            return true;
        }


    }

    public String extractSubject(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(SECRET_KEY.getBytes())
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    public String getStringClaim(String token, String key) {
        Claims claims = Jwts.parser()
                .setSigningKey(SECRET_KEY.getBytes())
                .parseClaimsJws(token)
                .getBody();
        return claims.get(key, String.class);
    }
}
