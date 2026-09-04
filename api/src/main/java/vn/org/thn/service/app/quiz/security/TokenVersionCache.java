package vn.org.thn.service.app.quiz.security;

/**
 * Read-through cache for the per-request account state {@link JwtAuthFilter}/{@link
 * PublicLanguageAdminGuardFilter} need on EVERY request (tokenVersion + active flag) - added
 * 2026-09-04 per the user's explicit request: {@code JwtAuthFilter} hitting the DB on every
 * single request (see its "tokenVersion re-check" javadoc) is correct but not free, and this
 * project currently has no distributed cache (RAM only for now - the user plans to introduce
 * Redis later). This interface is the seam that swap targets: {@link InMemoryTokenVersionCache}
 * is the only implementation today, but a future {@code RedisTokenVersionCache implements
 * TokenVersionCache} is a drop-in replacement - nothing outside this package (or the callers
 * below) needs to change.
 * <p>
 * <b>Correctness rule - EVICT on every mutation, never update-in-place, no TTL:</b> the whole
 * point of tokenVersion + force-logout (see {@code AuthService}'s javadoc) is that a logout/
 * deactivate/password-change/delete takes effect on the VERY NEXT request, not "eventually" or
 * "after a cache TTL expires". So every code path that changes a Parent/Student/Admin's {@code
 * tokenVersion} or {@code active} flag MUST call {@link #evict} for that account in the SAME
 * transaction/method (see {@code AuthService#invalidateSessions}, {@code
 * AdminParentService#setActive}, {@code AdminParentService#deleteCascade}) - the next request for
 * that account then misses the cache, re-reads the DB, and re-populates with the fresh state.
 * Deliberately no TTL-based expiry: a TTL would reintroduce exactly the "stale token still works
 * for up to N seconds after force-logout" window this whole mechanism exists to close.
 * <p>
 * Callers ({@link JwtAuthFilter}, {@link PublicLanguageAdminGuardFilter}) do NOT cache a "row
 * does not exist" result - only rows that were found get cached (see each filter's own cache-miss
 * handling) - a request for an already-deleted account simply keeps missing the cache and hitting
 * the DB every time, which is fine (self-limiting: real clients stop sending a token for a
 * deleted account almost immediately) and avoids needing a second "negative cache" invalidation
 * path for something that can never come back to life anyway.
 */
public interface TokenVersionCache {

    /** Cached state for one account - {@code active} is only meaningful for {@link Role#PARENT} (Student/Admin rows have no flag of their own, see {@code Parent#isActive()}'s javadoc); callers for Student/Admin always cache {@code active=true}. */
    record CachedAccountState(int tokenVersion, boolean active) {
    }

    /** Cache lookup - null on a cache miss (never cached yet, or evicted since). Callers must fall back to a DB read on null. */
    CachedAccountState get(Role role, Long userId);

    /** Populates/overwrites the cached state for one account - called by a filter right after a DB read on a cache miss. */
    void put(Role role, Long userId, CachedAccountState state);

    /** Drops the cached state for one account, if any - see this interface's javadoc, "EVICT on every mutation". A no-op if nothing was cached for it. */
    void evict(Role role, Long userId);
}
