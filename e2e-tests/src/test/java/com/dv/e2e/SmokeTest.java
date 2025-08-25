package com.dv.e2e;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
class SmokeTest {
  @Test
  @Disabled("Enable once docker-compose stack is running to hit real endpoints")
  void placeholder(){ assertTrue(true); }
}
