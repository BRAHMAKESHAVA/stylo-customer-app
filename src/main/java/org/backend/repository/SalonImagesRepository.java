package org.backend.repository;

import org.backend.model.SalonImages;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SalonImagesRepository extends JpaRepository<SalonImages, Long> {

//    @Query("SELECT si FROM SalonImages si WHERE si.salonId IN :salonIds AND si.imageKey = 'frontView'")
//    List<SalonImages> findFrontViewImages(@Param("salonIds") List<Long> salonIds);
//
//    @Query("SELECT si FROM SalonImages si WHERE si.salonId = :salonId AND si.imageKey = 'frontView'")
//    Optional<SalonImages> findFrontViewImage(@Param("salonId") Long salonId);

    // ✅ Priority: frontView first, else any available image — case-insensitive
    @Query("""
        SELECT si FROM SalonImages si
        WHERE si.salonId IN :salonIds
        AND si.id IN (
            SELECT MIN(s2.id) FROM SalonImages s2
            WHERE s2.salonId IN :salonIds
            GROUP BY s2.salonId
        )
        ORDER BY
            CASE WHEN LOWER(si.imageKey) = 'frontview' THEN 0 ELSE 1 END ASC
        """)
    List<SalonImages> findFrontViewImages(@Param("salonIds") List<Long> salonIds);

    // ✅ Single salon: frontView first, else any available image — case-insensitive
    @Query("""
        SELECT si FROM SalonImages si
        WHERE si.salonId = :salonId
        ORDER BY
            CASE WHEN LOWER(si.imageKey) = 'frontview' THEN 0 ELSE 1 END ASC
        """)
    List<SalonImages> findImagesForSalon(@Param("salonId") Long salonId);

}

