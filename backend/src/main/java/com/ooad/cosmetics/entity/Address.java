package com.ooad.cosmetics.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "addresses")
public class Address extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "receiver_name", nullable = false, length = 150)
    private String receiverName;

    @Column(nullable = false, length = 30)
    private String phone;

    @Column(name = "address_line", nullable = false, length = 255)
    private String addressLine;

    @Column(length = 120)
    private String ward;

    @Column(length = 120)
    private String district;

    @Column(nullable = false, length = 120)
    private String city;

    @Column(name = "is_default", nullable = false)
    private boolean defaultAddress;

    // Public because AddressService creates new Address instances.
    // JPA also accepts a public no-argument constructor.
    public Address() {
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddressLine() {
        return addressLine;
    }

    public String getWard() {
        return ward;
    }

    public String getDistrict() {
        return district;
    }

    public String getCity() {
        return city;
    }

    public boolean isDefaultAddress() {
        return defaultAddress;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setAddressLine(String addressLine) {
        this.addressLine = addressLine;
    }

    public void setWard(String ward) {
        this.ward = ward;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setDefaultAddress(boolean defaultAddress) {
        this.defaultAddress = defaultAddress;
    }
}
