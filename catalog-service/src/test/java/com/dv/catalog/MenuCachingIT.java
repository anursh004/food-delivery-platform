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
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class MenuCachingIT {

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
    r.setName("TestR"); r.setCity("Pune");
    r = restaurants.save(r);
    restaurantId = r.getId();

    MenuItem m1 = new MenuItem();
    m1.setRestaurantId(restaurantId);
    m1.setItemName("Item1"); m1.setPrice(new BigDecimal("100.00")); m1.setAvailable(true);
    menus.save(m1);
  }

  @Test
  void etagAndCache_works_with_refresh() throws Exception {
    // First call -> 200 + ETag
    String etag = mvc.perform(get("/restaurants/{id}/menu", restaurantId))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ETAG, notNullValue()))
        .andReturn().getResponse().getHeader(HttpHeaders.ETAG);

    // Second call with If-None-Match -> 304
    mvc.perform(get("/restaurants/{id}/menu", restaurantId).header(HttpHeaders.IF_NONE_MATCH, etag))
        .andExpect(status().isNotModified())
        .andExpect(header().string(HttpHeaders.ETAG, etag));

    // Modify DB (add an item), then refresh cache
    MenuItem m2 = new MenuItem();
    m2.setRestaurantId(restaurantId);
    m2.setItemName("Item2"); m2.setPrice(new BigDecimal("50.00")); m2.setAvailable(true);
    menus.save(m2);

    mvc.perform(post("/restaurants/{id}/menu/_refresh", restaurantId))
        .andExpect(status().isAccepted());

    // Now GET with old ETag should return 200 and a new ETag
    String newEtag = mvc.perform(get("/restaurants/{id}/menu", restaurantId).header(HttpHeaders.IF_NONE_MATCH, etag))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ETAG, notNullValue()))
        .andReturn().getResponse().getHeader(HttpHeaders.ETAG);

    org.junit.jupiter.api.Assertions.assertNotEquals(etag, newEtag);
  }
}
