package com.dv.catalog.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@EnableCaching
public class CacheConfig {

  public static final String MENU_CACHE = "menuByRestaurant";

  @Bean
  public CaffeineCacheManager caffeineCacheManager() {
    CaffeineCacheManager mgr = new CaffeineCacheManager();
    mgr.setCacheNames(Collections.singletonList(MENU_CACHE));
    // Simple spec: 1000 entries, 10m TTL
    mgr.setCacheSpecification("maximumSize=1000,expireAfterWrite=10m");
    return mgr;
  }

  @Bean
  public RedisCacheManager redisCacheManager(RedisConnectionFactory factory) {
    RedisCacheConfiguration cfg = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(30));
    return RedisCacheManager.builder(factory)
        .cacheDefaults(cfg)
        .initialCacheNames(Collections.singleton(MENU_CACHE))
        .build();
  }

  @Bean
  public CacheManager twoLevelCacheManager(
      @Qualifier("caffeineCacheManager") CacheManager l1,
      @Qualifier("redisCacheManager") CacheManager l2) {
    return new TwoLevelCacheManager(l1, l2);
  }

  static class TwoLevelCacheManager implements CacheManager {
    private final CacheManager l1;
    private final CacheManager l2;
    private final Map<String, Cache> caches = new ConcurrentHashMap<>();
    TwoLevelCacheManager(CacheManager l1, CacheManager l2){ this.l1=l1; this.l2=l2; }

    @Override
    public Cache getCache(String name) {
      return caches.computeIfAbsent(name, n -> new TwoLevelCache(l1.getCache(n), l2.getCache(n)));
    }

    @Override
    public Collection<String> getCacheNames() {
      return l1.getCacheNames();
    }
  }

  static class TwoLevelCache implements Cache {
    private final Cache l1;
    private final Cache l2;

    TwoLevelCache(Cache l1, Cache l2){ this.l1=l1; this.l2=l2; }

    @Override public String getName(){ return l1.getName(); }
    @Override public Object getNativeCache(){ return this; }

    @Override
    public ValueWrapper get(Object key) {
      ValueWrapper v = l1.get(key);
      if (v != null) return v;
      v = l2.get(key);
      if (v != null) {
        // populate L1
        l1.put(key, v.get());
      }
      return v;
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
      T v = l1.get(key, type);
      if (v != null) return v;
      v = l2.get(key, type);
      if (v != null) {
        l1.put(key, v);
      }
      return v;
    }

    @Override
    public <T> T get(Object key, java.util.concurrent.Callable<T> valueLoader) {
      ValueWrapper present = get(key);
      if (present != null) {
        @SuppressWarnings("unchecked")
        T casted = (T) present.get();
        return casted;
      }
      try {
        T loaded = valueLoader.call();
        put(key, loaded);
        return loaded;
      } catch (Exception e) {
        throw new IllegalStateException(e);
      }
    }

    @Override
    public void put(Object key, Object value) {
      l1.put(key, value);
      l2.put(key, value);
    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
      ValueWrapper prev = get(key);
      if (prev == null) {
        put(key, value);
      }
      return prev;
    }

    @Override
    public void evict(Object key) {
      l1.evict(key);
      l2.evict(key);
    }

    @Override
    public void clear() {
      l1.clear();
      l2.clear();
    }
  }
}
