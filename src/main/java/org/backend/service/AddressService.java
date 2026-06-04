package org.backend.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.backend.dto.request.CreateAddressRequest;
import org.backend.dto.request.UpdateAddressRequest;
import org.backend.dto.response.AddressResponse;
import org.backend.enums.AddressType;
import org.backend.exception.BadRequestException;
import org.backend.exception.ResourceNotFoundException;
import org.backend.model.Address;
import org.backend.projection.NearbyAddressProjection;
import org.backend.repository.AddressRepository;
import org.backend.repository.CustomerRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.backend.enums.CountryCode.validateCountryCode;

/**
 * Service class for managing customer addresses.
 * Provides business logic for creating, updating, deleting, and retrieving customer addresses,
 * including validation and default address management.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AddressService {

    private final AddressRepository addressRepository;
    private final CustomerRepository customerRepository;
    private final AuthService authService;

    @Value("${address.max.count}")
    private int maxAddressCount;

    /**
     * Creates a new address for the specified customer.
     * Validates customer existence, address count limits, and country code.
     * Handles default address logic by resetting other defaults if this is set as default.
     *
     * @param customerId the ID of the customer
     * @param address the address details to create
     * @return the created address as AddressDTO
     * @throws ResourceNotFoundException if customer not found
     * @throws BadRequestException if maximum address count exceeded
     */
    // CREATE ADDRESS
    public AddressResponse createAddress(Long customerId, CreateAddressRequest address) {
        // Validate that the logged-in customer is authorized to access this customer record
        authService.validateCustomerAccess(customerId);

        long count = addressRepository.countByCustomerId(customerId);

        if (count >= maxAddressCount)
            throw new BadRequestException("Maximum " + maxAddressCount + " addresses allowed per customer");

        // Validate country code
        validateCountryCode(address.getCountryCode());

        // Validate only one HOME and one WORK before creating
        validateUniqueAddressType(customerId, address.getAddressType(), null);

        // Handle if it is null in the request - default to false
        if (address.getIsDefault() == null)
            address.setIsDefault(false);

        // Handle if it is null in the request - default to true
        if (address.getIsSelected() == null)
            address.setIsSelected(true);

        Address newAddress = new Address();
        newAddress.setCustomerId(customerId);
        BeanUtils.copyProperties(address, newAddress);

        if (newAddress.getIsDefault())
            addressRepository.resetDefaultForCustomer(customerId);

        if (newAddress.getIsSelected())
            addressRepository.resetSelectedForCustomer(customerId);

        AddressResponse addressDTO = new AddressResponse();
        BeanUtils.copyProperties(addressRepository.save(newAddress), addressDTO);

        return addressDTO;
    }

    /**
     * Updates an existing address for the specified customer.
     * Only updates non-null fields from the DTO.
     *
     * @param customerId the ID of the customer
     * @param addressId the ID of the address to update
     * @param dto the address update details
     * @return the updated address as AddressDTO
     * @throws ResourceNotFoundException if customer or address not found
     */
    // UPDATE ADDRESS
    public AddressResponse updateAddress(Long customerId, Long addressId, UpdateAddressRequest dto) {
        // Validate that the logged-in customer is authorized to access this customer record
        authService.validateCustomerAccess(customerId);

        Address existingAddress = addressRepository
                .findByAddressIdAndCustomerId(addressId, customerId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Address not found for this customer")
                );

        if (dto.getCustomerName() != null)
            existingAddress.setCustomerName(dto.getCustomerName());

        if (dto.getHouseNumber() != null)
            existingAddress.setHouseNumber(dto.getHouseNumber());

        if (dto.getBuildingName() != null)
            existingAddress.setBuildingName(dto.getBuildingName());

        if (dto.getArea() != null)
            existingAddress.setArea(dto.getArea());

        if (dto.getLandmark() != null)
            existingAddress.setLandmark(dto.getLandmark());

        if (dto.getCity() != null)
            existingAddress.setCity(dto.getCity());

        if (dto.getState() != null)
            existingAddress.setState(dto.getState());

        if (dto.getCountryCode() != null) {
            validateCountryCode(dto.getCountryCode());
            existingAddress.setCountryCode(dto.getCountryCode());
        }

        if (dto.getPinCode() != null)
            existingAddress.setPinCode(dto.getPinCode());

        if (dto.getLatitude() != null)
            existingAddress.setLatitude(dto.getLatitude());

        if (dto.getLongitude() != null)
            existingAddress.setLongitude(dto.getLongitude());

        if (dto.getAddressType() != null) {
            // Validate only one HOME and one WORK excluding current address
            validateUniqueAddressType(customerId, dto.getAddressType(), addressId);
            existingAddress.setAddressType(dto.getAddressType());
        }

        if (dto.getLabelName() != null)
            existingAddress.setLabelName(dto.getLabelName());

        if (dto.getIsDefault() != null && dto.getIsDefault()) {
            addressRepository.resetDefaultForCustomer(customerId, addressId);
            existingAddress.setIsDefault(dto.getIsDefault());
        }

        if (dto.getIsSelected() != null && dto.getIsSelected()) {
            addressRepository.resetSelectedForCustomer(customerId, addressId);
            existingAddress.setIsSelected(dto.getIsSelected());
        }

        AddressResponse addressDTO = new AddressResponse();
        BeanUtils.copyProperties(addressRepository.save(existingAddress), addressDTO);

        return addressDTO;
    }

    /**
     * Deletes an address for the specified customer.
     * Prevents deletion of default addresses.
     *
     * @param customerId the ID of the customer
     * @param addressId the ID of the address to delete
     * @throws ResourceNotFoundException if customer or address not found
     * @throws BadRequestException if trying to delete a default address
     */
    // DELETE ADDRESS
    public void deleteAddress(Long customerId, Long addressId) {
        // Validate that the logged-in customer is authorized to access this customer record
        authService.validateCustomerAccess(customerId);

        Address address = addressRepository
                .findByAddressIdAndCustomerId(addressId, customerId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Address not found for this customer")
                );

        if (address.getIsDefault())
            throw new BadRequestException("Default address cannot be deleted");

        addressRepository.delete(address);
    }

    /**
     * Retrieves a specific address by ID for the specified customer.
     *
     * @param customerId the ID of the customer
     * @param addressId the ID of the address to retrieve
     * @return the address as AddressDTO
     * @throws ResourceNotFoundException if customer or address not found
     */
    // GET ADDRESS BY ID
    public AddressResponse getAddressById(Long customerId, Long addressId) {
        // Validate that the logged-in customer is authorized to access this customer record
        authService.validateCustomerAccess(customerId);

        Address customerAddress = addressRepository
                .findByAddressIdAndCustomerId(addressId, customerId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Address not found for the given customerId and addressId"
                        )
                );

        AddressResponse addressDTO = new AddressResponse();
        BeanUtils.copyProperties(customerAddress, addressDTO);

        return addressDTO;
    }

    /**
     * Retrieves all addresses for the specified customer.
     *
     * @param customerId the ID of the customer
     * @return list of addresses as AddressDTO
     * @throws ResourceNotFoundException if customer not found
     */
    // GET ALL ADDRESSES
    public List<AddressResponse> getAllAddresses(Long customerId) {
        // Validate that the logged-in customer is authorized to access this customer record
        authService.validateCustomerAccess(customerId);

        List<Address> allCustomerAddresses = addressRepository.findByCustomerIdOrderByUpdatedAtDesc(customerId);

        //return allCustomerAddresses;
        return allCustomerAddresses.stream()
                .map(address -> {
                    AddressResponse dto = new AddressResponse();
                    BeanUtils.copyProperties(address, dto);
                    return dto;
                })
                .toList();
    }

    private void validateUniqueAddressType(Long customerId, AddressType addressType, Long addressId) {

        // Multiple OTHER addresses are allowed
        if (addressType == AddressType.OTHER) {
            return;
        }

        boolean exists = (addressId == null)
                ? addressRepository.existsByCustomerIdAndAddressType(customerId, addressType)
                : addressRepository.existsByCustomerIdAndAddressTypeAndAddressIdNot(customerId, addressType, addressId);

        if (exists) {
            throw new BadRequestException(String.format("Only one %s address is allowed per customer", addressType));
        }
    }

    /**
     * Retrieves nearby addresses based on provided latitude and longitude.
     * Uses a simple bounding box approach to find addresses within a 1 km radius.
     *
     * @param latitude the latitude of the location
     * @param longitude the longitude of the location
     * @return list of nearby addresses with distance information
     */
    public List<AddressResponse> getNearbyAddresses(Long customerId, double latitude, double longitude) {
        // Validate that the logged-in customer is authorized to access this customer record
        authService.validateCustomerAccess(customerId);

        double radiusKm = 1.0;

        double latDelta = radiusKm / 111.0;
        double lonDelta = radiusKm /
                (111.0 * Math.cos(Math.toRadians(latitude)));

        double minLat = latitude - latDelta;
        double maxLat = latitude + latDelta;

        double minLon = longitude - lonDelta;
        double maxLon = longitude + lonDelta;

        List<NearbyAddressProjection> addresses =
                addressRepository.findNearbyAddresses(
                        customerId,
                        latitude,
                        longitude,
                        minLat,
                        maxLat,
                        minLon,
                        maxLon,
                        radiusKm
                );
        if (!addresses.isEmpty()) {
            Long addressId = addresses.getFirst().getAddressId();
            // Unselect all other addresses
            addressRepository.resetSelectedForCustomer(customerId, addressId);
            // Select the nearest address
            addressRepository.updateSelectedAddress(addressId, true);
        }
        return addresses.stream()
                .map(a -> AddressResponse.builder()
                        .addressId(a.getAddressId())
                        .customerId(a.getCustomerId())
                        .customerName(a.getCustomerName())
                        .houseNumber(a.getHouseNumber())
                        .buildingName(a.getBuildingName())
                        .area(a.getArea())
                        .landmark(a.getLandmark())
                        .city(a.getCity())
                        .state(a.getState())
                        .pinCode(a.getPinCode())
                        .latitude(a.getLatitude())
                        .longitude(a.getLongitude())
                        .addressType(a.getAddressType())
                        .labelName(a.getLabelName())
                        .isDefault(a.getIsDefault())
                        .isSelected(true)
                        .distance(Math.round(a.getDistanceKm() * 100.0) / 100.0)
                        .build())
                .toList();


    }
}