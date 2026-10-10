package com.ooad.cosmetics.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "carts")
public class Cart extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CartStatus status = CartStatus.ACTIVE;

    public Cart() {
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public CartStatus getStatus() {
        return status;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setStatus(CartStatus status) {
        this.status = status;
    }
}
