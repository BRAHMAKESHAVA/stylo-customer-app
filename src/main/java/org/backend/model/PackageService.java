package org.backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "package_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackageService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "package_id", nullable = false)
    private Long packageId;

    @Column(name = "service_id", nullable = false)
    private Long serviceId;
}