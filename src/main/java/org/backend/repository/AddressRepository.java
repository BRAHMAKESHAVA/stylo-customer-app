package org.backend.repository;

import org.backend.enums.AddressType;
import org.backend.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
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

    @Modifying @Query("UPDATE Address a SET a.isDefault = false WHERE a.customerId = :customerId AND a.addressId <> :addressId")
    void resetDefaultForCustomer(Long customerId, Long addressId);

    @Modifying
    @Query("UPDATE Address a SET a.isSelected = false WHERE a.customerId = :customerId AND a.addressId <> :addressId")
    void resetSelectedForCustomer(Long customerId, Long addressId);

    long countByCustomerId(Long customerId);

    // Check if HOME/WORK address already exists for create
    boolean existsByCustomerIdAndAddressType(Long customerId, AddressType addressType);

    // Check if HOME/WORK exists excluding current address during update
    boolean existsByCustomerIdAndAddressTypeAndAddressIdNot(Long customerId, AddressType addressType, Long addressId);
}