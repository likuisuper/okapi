package org.folio.okapi.util;

import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.Logger;
import org.folio.okapi.common.OkapiLogger;

public class TokenCache {

  public static final long TTL = 3 * 60 * 1000L;
  
  private Map<String, CacheEntry> cache = new HashMap<>();
  private static final Logger logger = OkapiLogger.get(TokenCache.class);
  
  /**
   * Cache an entry.
   * @param method HTTP method
   * @param path path pattern 
   * @param userId X-Okapi-User-Id header to cache
   * @param xokapiPerms X-Okapi-Permissions header to cache
   * @param token access token to cache
   */
  public void put(String method, String path, String userId,
      String xokapiPerms, String keyToken, String token) {
    long now = System.currentTimeMillis();
    CacheEntry entry = new CacheEntry(token, userId, xokapiPerms, now + TTL);
    String key = genKey(method, path, keyToken, userId);
    logger.info("Caching: " + key + " " + token);
    cache.put(key, entry);
  }
  
  /**
   * Get a cached entry.
   * @param method HTTP method
   * @param path path pattern
   * @param token X-Okapi-Token header
   * @param userId X-Okapi-User-Id header 
   * @return cache entry or null
   */
  public CacheEntry get(String method, String path, String token, String userId) {
    String key = genKey(method, path, token, userId);
    CacheEntry ret = cache.get(genKey(method, path, token, userId));
    if (ret == null) {
      logger.info("Cache Miss: {}", key);
      return ret;
    } else if (ret.isExpired()) {
      logger.info("Cache Hit (Expired): {}", key);
      cache.remove(key);
      return null;
    } else {
      logger.info("Cache Hit: {} -> {}", key, ret.token);
      return ret;
    }
  }
  
  private String genKey(String method, String path, String token, String userId) {
    return method + "|" + path + "|" + token + "|" + userId;
  }
  
  public static class CacheEntry {
    public final String token;
    public final String xokapiPermissions;
    public final String xokapiUserid;
    public final long expires;
    
    private CacheEntry() {
      throw new IllegalArgumentException();
      // Should never get here.
    }
    
    /**
     * Create a cache entry.
     * @param token the access token to cache
     * @param xokapiUserid the X-Okapi-User-Id header
     * @param xokapiPermissions the X-Okapi-Permissions header
     * @param expires instant in ms since epoch when this cache entry expires
     */
    public CacheEntry(String token, String xokapiUserid, String xokapiPermissions, long expires) {
      this.token = token;
      this.xokapiPermissions = xokapiPermissions;
      this.xokapiUserid = xokapiUserid;
      this.expires = expires;
    }
    
    public boolean isExpired() {
      return System.currentTimeMillis() > expires;
    }
  }
}
