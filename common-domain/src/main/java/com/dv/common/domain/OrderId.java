package com.dv.common.domain;
import java.util.Objects;
public final class OrderId {
  private final String value;
  public OrderId(String value) {
    if (value == null || value.isBlank()) throw new IllegalArgumentException("OrderId cannot be blank");
    this.value = value;
  }
  public String get() { return value; }
  @Override public String toString(){ return value; }
  @Override public boolean equals(Object o){ return (o instanceof OrderId) && Objects.equals(value, ((OrderId)o).value); }
  @Override public int hashCode(){ return Objects.hash(value); }
}
