package org.backend.repository;

import jakarta.persistence.LockModeType;
import org.backend.model.SalonResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SalonResourceRepository extends JpaRepository<SalonResource, Long> {
    boolean existsBySalonId(Long salonId);

    Optional<SalonResource> findBySalonId(Long salonId);

    Optional<SalonResource> findByIdAndSalonId(Long id, Long salonId);

    //@Lock(LockModeType.PESSIMISTIC_WRITE)
    //@Query("SELECT sr FROM SalonResource sr WHERE sr.salonId = :salonId")
    //SalonResource lockSalonResource(@Param("salonId") Long salonId);
}