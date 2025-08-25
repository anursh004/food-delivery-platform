package com.dv.catalog.domain;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;
@Entity
@Table(name="menus")
public class MenuItem {
  @Id @GeneratedValue(strategy=GenerationType.UUID)
  private UUID id;
  private UUID restaurantId;
  private String itemName;
  private BigDecimal price;
  private boolean isAvailable;
  // getters/setters
  public UUID getId(){return id;} public void setId(UUID id){this.id=id;}
  public UUID getRestaurantId(){return restaurantId;} public void setRestaurantId(UUID r){this.restaurantId=r;}
  public String getItemName(){return itemName;} public void setItemName(String i){this.itemName=i;}
  public BigDecimal getPrice(){return price;} public void setPrice(BigDecimal p){this.price=p;}
  public boolean isAvailable(){return isAvailable;} public void setAvailable(boolean a){this.isAvailable=a;}
}
