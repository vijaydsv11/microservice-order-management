package com.microservices.inventoryservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "inventory")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false)
    private Integer quantityAvailable;

    @Column(nullable = false)
    private Integer quantityReserved;

    @Column(nullable = false)
    private Integer quantityAllocated;

    public Integer getQuantityFree() {
        return quantityAvailable - quantityReserved - quantityAllocated;
    }

    public void allocateQuantity(Integer quantity) {
        if (quantity > getQuantityFree()) {
            throw new RuntimeException("Insufficient quantity available for allocation: " + productName);
        }
        this.quantityAllocated += quantity;
    }

    public void reserveQuantity(Integer quantity) {
        if (quantity > getQuantityFree()) {
            throw new RuntimeException("Insufficient quantity available for reservation: " + productName);
        }
        this.quantityReserved += quantity;
    }

    public void releaseAllocatedQuantity(Integer quantity) {
        this.quantityAllocated -= quantity;
    }

    public void releaseReservedQuantity(Integer quantity) {
        this.quantityReserved -= quantity;
    }
}
