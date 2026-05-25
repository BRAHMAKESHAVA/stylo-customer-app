package org.backend.repository;

import org.backend.model.PackageService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PackageServiceRepository
        extends JpaRepository<PackageService, Long> {

    List<PackageService> findByPackageId(Long packageId);

    void deleteByPackageId(Long packageId);
}