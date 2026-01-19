package com.organics.products.entity;

import jakarta.persistence.*;
import lombok.Data;



@Entity
@Data
@Table(name = "wishlistitems")
public class WishListItems {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne
    @JoinColumn(name = "wishlist_id")
    private Wishlist wishlist;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WishListItems)) return false;
        WishListItems that = (WishListItems) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }


}
