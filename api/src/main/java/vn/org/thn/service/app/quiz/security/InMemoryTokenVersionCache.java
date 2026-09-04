package vn.org.thn.service.app.quiz.security;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The only {@link TokenVersionCache} implementation today - a plain in-process {@link
 * ConcurrentHashMap}, no TTL, no eviction policy other than the explicit {@link #evict} calls
 * documented on the interface. See {@link TokenVersionCache}'s javadoc for the full design
 * rationale (why RAM now / Redis later, why no TTL).
 * <p>
 * Being in-process only, this cache is naturally per-instance: if/when quiz-service ever runs as
 * more than one replica behind a load balancer, an evict on instance A does NOT clear instance
 * B's copy of the same entry, so a force-logout could stay live on the wrong instance until that
 * entry happens to be evicted there too by its own later mutation. This is exactly the gap a
 * shared cache (Redis) closes - swapping this class for a {@code RedisTokenVersionCache
 * implements TokenVersionCache} (same interface, no caller changes) is the intended fix once
 * quiz-service actually runs multi-instance; single-instance deployments (today) are unaffected.
 */
@Component
public class InMemoryTokenVersionCache implements TokenVersionCache {

    private final ConcurrentMap<String, CachedAccountState> cache = new ConcurrentHashMap<>();

    @Override
    public CachedAccountState get(Role role, Long userId) {
        return cache.get(key(role, userId));
    }

    @Override
    public void put(Role role, Long userId, CachedAccountState state) {
        cache.put(key(role, userId), state);
    }

    @Override
    public void evict(Role role, Long userId) {
        cache.remove(key(role, userId));
    }

    private static String key(Role role, Long userId) {
        return role.name() + ":" + userId;
    }
}
