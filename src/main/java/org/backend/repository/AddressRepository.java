package org.backend.repository;

import org.backend.enums.AddressType;
import org.backend.model.Address;
import org.backend.projection.NearbyAddressProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing Address entities.
 * Provides CRUD operations and custom queries for customer addresses.
 */
@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    Optional<Address> findByAddressIdAndCustomerId(Long addressId, Long customerId);

    List<Address> findByCustomerIdOrderByUpdatedAtDesc(Long customerId);

    @Modifying
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.customerId = :customerId AND a.isDefault = true")
    void resetDefaultForCustomer(Long customerId);

    @Modifying
    @Query("UPDATE Address a SET a.isSelected = false WHERE a.customerId = :customerId AND a.isSelected = true")
    void resetSelectedForCustomer(Long customerId);

    @Modifying
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.customerId = :customerId AND a.addressId <> :addressId")
    void resetDefaultForCustomer(Long customerId, Long addressId);

    @Modifying
    @Query("UPDATE Address a SET a.isSelected = false WHERE a.customerId = :customerId AND a.addressId <> :addressId")
    void resetSelectedForCustomer(Long customerId, Long addressId);

    long countByCustomerId(Long customerId);

    // Check if HOME/WORK address already exists for create
    boolean existsByCustomerIdAndAddressType(Long customerId, AddressType addressType);

    // Check if HOME/WORK exists excluding current address during update
    boolean existsByCustomerIdAndAddressTypeAndAddressIdNot(Long customerId, AddressType addressType, Long addressId);

    @Query(value = """
            SELECT *
            FROM (
                SELECT
                    a.address_id AS addressId,
                    a.customer_id AS customerId,
                    a.customer_name AS customerName,
                    a.house_number AS houseNumber,
                    a.building AS buildingName,
                    a.area AS area,
                    a.landmark AS landmark,
                    a.city AS city,
                    a.state AS state,
                    a.pin_code AS pinCode,
                    a.latitude AS latitude,
                    a.longitude AS longitude,
            
                    (
                        6371 * acos(
                            cos(radians(:latitude))
                            * cos(radians(a.latitude))
                            * cos(radians(a.longitude) - radians(:longitude))
                            + sin(radians(:latitude))
                            * sin(radians(a.latitude))
                        )
                    ) AS distanceKm
            
                FROM address a
                WHERE a.customer_id = :customerId
                  AND a.latitude BETWEEN :minLat AND :maxLat
                  AND a.longitude BETWEEN :minLon AND :maxLon
            ) x
            WHERE x.distanceKm <= :distanceKm
            ORDER BY x.distanceKm
            """,
            nativeQuery = true)
    List<NearbyAddressProjection> findNearbyAddresses(
            @Param("customerId") Long customerId,
            @Param("latitude") double latitude,
            @Param("longitude") double longitude,
            @Param("minLat") double minLat,
            @Param("maxLat") double maxLat,
            @Param("minLon") double minLon,
            @Param("maxLon") double maxLon,
            @Param("distanceKm") double distanceKm
    );
}