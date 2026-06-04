package org.backend.repository;

import org.backend.model.PartnerDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PartnerRepository extends JpaRepository<PartnerDetails, Long> {

    boolean existsByMobile(String mobile);

    Optional<PartnerDetails> findByMobile(String mobile);

    Optional<PartnerDetails> findByUserId(Long userId);
}

