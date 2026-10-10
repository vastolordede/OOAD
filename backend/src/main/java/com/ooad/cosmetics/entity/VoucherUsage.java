package com.ooad.cosmetics.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "voucher_usages")
public class VoucherUsage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "voucher_id", nullable = false)
    private Voucher voucher;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Column(name = "discount_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal discountAmount;

    public VoucherUsage() {
    }

    public Long getId() {
        return id;
    }

    public Voucher getVoucher() {
        return voucher;
    }

    public User getUser() {
        return user;
    }

    public Order getOrder() {
        return order;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setVoucher(Voucher voucher) {
        this.voucher = voucher;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }
}
