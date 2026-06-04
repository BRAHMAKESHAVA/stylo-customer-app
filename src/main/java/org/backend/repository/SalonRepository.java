package org.backend.repository;

import org.backend.model.SalonDetails;
import org.backend.projection.NearBySalonsProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SalonRepository extends JpaRepository<SalonDetails, Long> {

    @Query(value = """
                SELECT *
                FROM (
                    SELECT 
                        s.salon_id AS salonId,
                        s.partner_id AS partnerId,
                        s.salon_name AS salonName,
                        s.latitude AS latitude,
                        s.longitude AS longitude,
                        s.address_line1 AS addressLine1,
                        s.address_line2 AS addressLine2,
                        s.landmark AS landmark,
                        s.city AS city,
                        s.state AS state,
                        s.zip_code AS zipCode,
                        s.country AS country,
                        s.working_days AS workingDays,
                        s.working_hours_start AS workingHoursStart,
                        s.working_hours_end AS workingHoursEnd,
                        (6371 * acos(
                            cos(radians(:lat)) * cos(radians(s.latitude)) *
                            cos(radians(s.longitude) - radians(:lon)) +
                            sin(radians(:lat)) * sin(radians(s.latitude))
                        )) AS distanceKm
                    FROM salon s
                    WHERE s.latitude BETWEEN :minLat AND :maxLat
                      AND s.longitude BETWEEN :minLon AND :maxLon
                ) AS nearby
                WHERE nearby.distanceKm <= :distanceKm
                ORDER BY nearby.distanceKm ASC
            """, nativeQuery = true)
    List<NearBySalonsProjection> findNearbySalons(
            @Param("lat") double lat,
            @Param("lon") double lon,
            @Param("minLat") double minLat,
            @Param("maxLat") double maxLat,
            @Param("minLon") double minLon,
            @Param("maxLon") double maxLon,
            @Param("distanceKm") double distanceKm
    );

    @Query(value = """
                SELECT *
                FROM (
                    SELECT 
                        s.salon_id AS salonId,
                        s.partner_id AS partnerId,
                        s.salon_name AS salonName,
                        s.latitude AS latitude,
                        s.longitude AS longitude,
                        s.address_line1 AS addressLine1,
                        s.address_line2 AS addressLine2,
                        s.landmark AS landmark,
                        s.city AS city,
                        s.state AS state,
                        s.zip_code AS zipCode,
                        s.country AS country,
                        s.working_days AS workingDays,
                        s.working_hours_start AS workingHoursStart,
                        s.working_hours_end AS workingHoursEnd,
                        (6371 * acos(
                            cos(radians(:lat)) * cos(radians(s.latitude)) *
                            cos(radians(s.longitude) - radians(:lon)) +
                            sin(radians(:lat)) * sin(radians(s.latitude))
                        )) AS distanceKm
                    FROM salon s
                    WHERE s.latitude BETWEEN :minLat AND :maxLat
                      AND s.longitude BETWEEN :minLon AND :maxLon
                ) AS nearby
                WHERE nearby.distanceKm <= :distanceKm
                ORDER BY nearby.distanceKm ASC
            """,
            countQuery = """
                        SELECT COUNT(*) 
                        FROM (
                            SELECT 
                                (6371 * acos(
                                    cos(radians(:lat)) * cos(radians(s.latitude)) *
                                    cos(radians(s.longitude) - radians(:lon)) +
                                    sin(radians(:lat)) * sin(radians(s.latitude))
                                )) AS distanceKm
                            FROM salon s
                            WHERE s.latitude BETWEEN :minLat AND :maxLat
                              AND s.longitude BETWEEN :minLon AND :maxLon
                        ) AS nearby
                        WHERE nearby.distanceKm <= :distanceKm
                    """,
            nativeQuery = true)
    Page<NearBySalonsProjection> findNearbySalonsWithPagination(
            double lat,
            double lon,
            double minLat,
            double maxLat,
            double minLon,
            double maxLon,
            double distanceKm,
            Pageable pageable
    );

    @Query(value = """
            SELECT *
            FROM (
                  SELECT 
                        s.salon_id AS salonId,
                        s.partner_id AS partnerId,
                        s.salon_name AS salonName,
                        s.latitude AS latitude,
                        s.longitude AS longitude,
                        s.address_line1 AS addressLine1,
                        s.address_line2 AS addressLine2,
                        s.landmark AS landmark,
                        s.city AS city,
                        s.state AS state,
                        s.zip_code AS zipCode,
                        s.country AS country,
                        s.working_days AS workingDays,
                        s.working_hours_start AS workingHoursStart,
                        s.working_hours_end AS workingHoursEnd,
                        (6371 * acos(
                        cos(radians(:lat)) * cos(radians(s.latitude)) *
                        cos(radians(s.longitude) - radians(:lon)) +
                        sin(radians(:lat)) * sin(radians(s.latitude))
                    )) AS distanceKm
                FROM salon s
                WHERE LOWER(s.salon_name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            ) AS nearby
            WHERE nearby.distanceKm <= :distanceKm
            ORDER BY nearby.distanceKm ASC
            LIMIT 10
            """, nativeQuery = true)
    List<NearBySalonsProjection> searchNearbySalons(
            double lat,
            double lon,
            String keyword
    );


    @Query(value = """
        SELECT *
        FROM (
            SELECT 
                s.salon_id AS salonId,
                s.partner_id AS partnerId,
                s.salon_name AS salonName,
                s.latitude AS latitude,
                s.longitude AS longitude,
                s.address_line1 AS addressLine1,
                s.address_line2 AS addressLine2,
                s.landmark AS landmark,
                s.city AS city,
                s.state AS state,
                s.zip_code AS zipCode,
                s.country AS country,
                s.working_days AS workingDays,
                s.working_hours_start AS workingHoursStart,
                s.working_hours_end AS workingHoursEnd,
                (6371 * acos(
                    cos(radians(:lat)) * cos(radians(s.latitude)) *
                    cos(radians(s.longitude) - radians(:lon)) +
                    sin(radians(:lat)) * sin(radians(s.latitude))
                )) AS distanceKm
            FROM salon s
             WHERE LOWER(s.salon_name) LIKE LOWER(CONCAT('%', :keyword, '%'))
        ) AS nearby
        ORDER BY nearby.distanceKm ASC
        LIMIT 10
        """, nativeQuery = true)
    List<NearBySalonsProjection> searchSalonsByKeyword(
            double lat,
            double lon,
            String keyword
    );


    @Query(value = """
            SELECT *
            FROM (
                SELECT 
                    s.salon_id AS salonId,
                    s.partner_id AS partnerId,
                    s.salon_name AS salonName,
                    s.latitude AS latitude,
                    s.longitude AS longitude,
                    s.address_line1 AS addressLine1,
                    s.address_line2 AS addressLine2,
                    s.landmark AS landmark,
                    s.city AS city,
                    s.state AS state,
                    s.zip_code AS zipCode,
                    s.country AS country,
                    s.working_days AS workingDays,
                    s.working_hours_start AS workingHoursStart,
                    s.working_hours_end AS workingHoursEnd,
                    (6371 * acos(
                        cos(radians(:lat)) * cos(radians(s.latitude)) *
                        cos(radians(s.longitude) - radians(:lon)) +
                        sin(radians(:lat)) * sin(radians(s.latitude))
                    )) AS distanceKm
                FROM salon s
                WHERE s.latitude BETWEEN :minLat AND :maxLat
                  AND s.longitude BETWEEN :minLon AND :maxLon
            ) AS nearby
            WHERE nearby.distanceKm <= :distanceKm
            ORDER BY nearby.distanceKm ASC
            LIMIT :size
        """, nativeQuery = true)
    List<NearBySalonsProjection> findPopularSalons(
            @Param("lat") double lat,
            @Param("lon") double lon,
            @Param("minLat") double minLat,
            @Param("maxLat") double maxLat,
            @Param("minLon") double minLon,
            @Param("maxLon") double maxLon,
            @Param("distanceKm") double distanceKm,
            @Param("size") int size
    );

    boolean existsBySalonIdAndPartnerPartnerId(Long salonId, Long partnerId);
}

