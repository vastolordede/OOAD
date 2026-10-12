package com.ooad.cosmetics.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "wishlist_items")
public class WishlistItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wishlist_id", nullable = false)
    private Wishlist wishlist;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    public WishlistItem() {
    }

    public Long getId() {
        return id;
    }

    public Wishlist getWishlist() {
        return wishlist;
    }

    public Product getProduct() {
        return product;
    }

    public void setWishlist(Wishlist wishlist) {
        this.wishlist = wishlist;
    }

    public void setProduct(Product product) {
        this.product = product;
    }
}
