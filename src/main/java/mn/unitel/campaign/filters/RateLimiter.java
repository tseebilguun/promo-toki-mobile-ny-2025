package mn.unitel.campaign.filters;

import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class RateLimiter {

    private final Map<String, RequestInfo> requests = new ConcurrentHashMap<>();

    private static final int LIMIT = 5;
    private static final long WINDOW_MS = 60_000;
    private static final long FREEZE_MS = 60_000;

    public boolean isAllowed(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }

        long now = Instant.now().toEpochMilli();
        RequestInfo info = requests.computeIfAbsent(key, k -> new RequestInfo(now));

        synchronized (info) {
            if (info.frozenUntil > now) {
                return false;
            }

            if (info.frozenUntil != 0 && info.frozenUntil <= now) {
                info.frozenUntil = 0;
                info.counter = 0;
                info.windowStart = now;
            }

            // new window?
            if (now - info.windowStart > WINDOW_MS) {
                info.windowStart = now;
                info.counter = 0;
            }

            if (info.counter >= LIMIT) {
                info.frozenUntil = now + FREEZE_MS;
                return false;
            }

            info.counter++;
            return true;
        }
    }

    private static class RequestInfo {
        long windowStart;
        int counter = 0;
        long frozenUntil = 0;

        RequestInfo(long now) {
            this.windowStart = now;
        }
    }
}
