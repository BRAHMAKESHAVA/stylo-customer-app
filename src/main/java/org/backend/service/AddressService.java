package org.backend.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.backend.dto.request.CreateAddressRequest;
import org.backend.dto.request.UpdateAddressRequest;
import org.backend.dto.response.AddressResponse;
import org.backend.exception.BadRequestException;
import org.backend.exception.ResourceNotFoundException;
import org.backend.model.Address;
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

        if (!customerRepository.existsByCustomerId(customerId))
            throw new ResourceNotFoundException("Customer not found with ID: " + customerId);

        long count = addressRepository.countByCustomerId(customerId);

        if (count >= maxAddressCount)
            throw new BadRequestException("Maximum " + maxAddressCount + " addresses allowed per customer");

        validateCountryCode(address.getCountryCode());

        if (address.getIsDefault() == null)
            address.setIsDefault(false);

        if (address.getIsDefault())
            addressRepository.resetDefaultForCustomer(customerId);

        address.setCustomerId(customerId);

        Address newAddress = new Address();
        BeanUtils.copyProperties(address, newAddress);

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
        if (!customerRepository.existsById(customerId))
            throw new ResourceNotFoundException("Customer not found with ID: " + customerId);

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

        if (dto.getAddressType() != null)
            existingAddress.setAddressType(dto.getAddressType());

        if (dto.getLabelName() != null)
            existingAddress.setLabelName(dto.getLabelName());

        if (dto.getIsDefault() != null && dto.getIsDefault()) {
            addressRepository.resetDefaultForCustomer(customerId, addressId);
            existingAddress.setIsDefault(dto.getIsDefault());
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
        if (!customerRepository.existsById(customerId))
            throw new ResourceNotFoundException("Customer not found with ID: " + customerId);

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
        if (!customerRepository.existsById(customerId))
            throw new ResourceNotFoundException("Customer not found with ID: " + customerId);

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
        if (!customerRepository.existsById(customerId))
            throw new ResourceNotFoundException("Customer not found with ID: " + customerId);

        List<Address> allCustomerAddresses = addressRepository.findByCustomerId(customerId);

        //return allCustomerAddresses;
        return allCustomerAddresses.stream()
                .map(address -> {
                    AddressResponse dto = new AddressResponse();
                    BeanUtils.copyProperties(address, dto);
                    return dto;
                })
                .toList();
    }
}