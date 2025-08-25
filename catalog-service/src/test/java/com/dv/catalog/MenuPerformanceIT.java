package com.dv.catalog;

import com.dv.catalog.domain.MenuItem;
import com.dv.catalog.domain.Restaurant;
import com.dv.catalog.repo.MenuItemRepository;
import com.dv.catalog.repo.RestaurantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class MenuPerformanceIT {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");
  @Container
  static GenericContainer<?> redis = new GenericContainer<>("redis:7").withExposedPorts(6379);

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", () -> postgres.getJdbcUrl());
    r.add("spring.datasource.username", () -> postgres.getUsername());
    r.add("spring.datasource.password", () -> postgres.getPassword());
    r.add("spring.data.redis.host", () -> redis.getHost());
    r.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
  }

  @Autowired MockMvc mvc;
  @Autowired RestaurantRepository restaurants;
  @Autowired MenuItemRepository menus;

  UUID restaurantId;

  @BeforeEach
  void seed() {
    menus.deleteAll();
    restaurants.deleteAll();
    Restaurant r = new Restaurant();
    r.setName("PerfR"); r.setCity("Bengaluru");
    r = restaurants.save(r);
    restaurantId = r.getId();
    for (int i=0;i<40;i++){
      MenuItem m = new MenuItem();
      m.setRestaurantId(restaurantId);
      m.setItemName("Item"+i);
      m.setPrice(new BigDecimal(50 + i));
      m.setAvailable(true);
      menus.save(m);
    }
  }

  @Test
  void p99_under_200ms_when_cached() throws Exception {
    // Warm cache
    mvc.perform(get("/restaurants/{id}/menu", restaurantId)).andExpect(status().isOk());

    int N = 200;
    long[] times = new long[N];
    String etag = mvc.perform(get("/restaurants/{id}/menu", restaurantId)).andReturn().getResponse().getHeader(HttpHeaders.ETAG);

    for (int i=0;i<N;i++){
      long t0 = System.nanoTime();
      mvc.perform(get("/restaurants/{id}/menu", restaurantId).header(HttpHeaders.IF_NONE_MATCH, etag))
          .andExpect(status().isNotModified());
      long t1 = System.nanoTime();
      times[i] = (t1 - t0) / 1_000_000; // ms
    }
    Arrays.sort(times);
    long p99 = times[(int)Math.floor(0.99 * (N-1))];
    assertTrue(p99 < 200, "P99 should be <200ms, got " + p99 + "ms");
  }
}
