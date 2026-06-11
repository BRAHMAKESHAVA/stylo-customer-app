package org.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "package")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Package {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "package_id")
    private Long packageId;

    @Column(name = "salon_id", nullable = false)
    private Long salonId;

    @Column(name = "package_name", nullable = false, length = 100)
    private String packageName;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "package_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal packagePrice;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
}