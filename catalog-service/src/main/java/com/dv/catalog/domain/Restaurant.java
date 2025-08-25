package com.dv.catalog.domain;
import jakarta.persistence.*;
import java.util.*;
@Entity
@Table(name="restaurants")
public class Restaurant {
  @Id @GeneratedValue(strategy=GenerationType.UUID)
  private UUID id;
  private String name;
  private String city;
  // getters/setters
  public UUID getId(){ return id; } public void setId(UUID id){ this.id=id; }
  public String getName(){ return name; } public void setName(String n){ this.name=n; }
  public String getCity(){ return city; } public void setCity(String c){ this.city=c; }
}
