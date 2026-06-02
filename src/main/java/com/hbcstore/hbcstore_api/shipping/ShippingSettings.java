package com.hbcstore.hbcstore_api.shipping;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "shipping_settings")
public class ShippingSettings {
    @Id
    private Long id = 1L;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal northFee = BigDecimal.valueOf(25000);

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal centralFee = BigDecimal.valueOf(35000);

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal southFee = BigDecimal.valueOf(45000);

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal freeShippingThreshold = BigDecimal.valueOf(1000000);

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
