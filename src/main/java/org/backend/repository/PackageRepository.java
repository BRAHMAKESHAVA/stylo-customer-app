package org.backend.repository;

import org.backend.model.Package;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PackageRepository extends JpaRepository<Package, Long> {

    Optional<Package> findBySalonIdAndPackageNameIgnoreCase(
            Long salonId,
            String packageName
    );

    List<Package> findBySalonId(Long salonId);

    List<Package> findByIsActiveTrue();

    List<Package> findBySalonIdAndIsActiveTrue(Long salonId);

    Optional<Package> findByPackageIdAndSalonId(
            Long packageId,
            Long salonId
    );

    Optional<Package> findByPackageIdAndSalonIdAndIsActiveTrue(
            Long packageId,
            Long salonId
    );
}