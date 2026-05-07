package org.backend.service;

import lombok.RequiredArgsConstructor;
import org.backend.dto.*;
import org.backend.exception.BadRequestException;
import org.backend.exception.DuplicateResourceException;
import org.backend.exception.ResourceNotFoundException;
import org.backend.model.SalonService;
import org.backend.model.ServiceCategory;
import org.backend.repository.CategoryRepository;
import org.backend.repository.SalonRepository;
import org.backend.repository.SalonServiceRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service class for managing salon services.
 * Provides business logic for creating, updating, retrieving, and managing salon services,
 * including grouping by categories and pagination support.
 */
@Service
@RequiredArgsConstructor
public class SalonServices {

    private final SalonServiceRepository salonServiceRepository;
    private final SalonRepository salonRepository;
    private final CategoryRepository categoryRepository;

    /**
     * Fetches all services for a given salon, grouped by category.
     */
    // GET SERVICES BY SALON
    public List<CategoryGroupDTO> getServicesBySalon(Long salonId) {
        if (!salonRepository.existsById(salonId)) {
            throw new ResourceNotFoundException("Salon not found");
        }

        List<ServiceCategory> categories = categoryRepository.findAll();

        Map<Long, String> categoryMap = categories.stream()
                .collect(Collectors.toMap(
                        ServiceCategory::getCategoryId,
                        ServiceCategory::getCategoryName
                ));

        List<SalonService> services = salonServiceRepository.findBySalonId(salonId);

        if (services.isEmpty()) {
            throw new ResourceNotFoundException("No services found for this salon");
        }

        Map<Long, List<SalonService>> groupedByCategory = services.stream()
                .collect(Collectors.groupingBy(SalonService::getCategoryId));

        return groupedByCategory.entrySet().stream()
                .map(entry -> new CategoryGroupDTO(
                        entry.getKey(),
                        categoryMap.get(entry.getKey()),
                        entry.getValue()
                ))
                .toList();
    }

    /**
     * Creates a new service for a salon.
     */
    // CREATE SERVICE
    public SalonServiceDTO createService(SalonServiceDTO request) {

        salonRepository.findById(request.getSalonId())
                .orElseThrow(() -> new ResourceNotFoundException("Salon not found"));

        categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        boolean exists = salonServiceRepository
                .existsBySalonIdAndServiceNameIgnoreCase(
                        request.getSalonId(),
                        request.getServiceName().trim()
                );

        if (exists) {
            throw new DuplicateResourceException(
                    "Service '" + request.getServiceName() + "' already exists for this salon"
            );
        }

        if (request.getIsActive() == null)
            request.setIsActive(true);

        SalonService service = new SalonService();
        BeanUtils.copyProperties(request, service);

        SalonServiceDTO dto = new SalonServiceDTO();
        BeanUtils.copyProperties(salonServiceRepository.save(service), dto);

        return dto;
    }

    /**
     * Updates an existing service.
     */
    // UPDATE SERVICE
    public SalonServiceDTO updateService(Long serviceId, UpdateServiceRequest request) {
        if (request.getSalonId() == null) {
            throw new BadRequestException("Salon ID is required to update the service.");
        }

        SalonService service = salonServiceRepository
                .findByServiceIdAndSalonId(serviceId, request.getSalonId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Service with ID " + serviceId +
                                        " not found for salon ID " + request.getSalonId()
                        )
                );

        if (request.getServiceName() != null) {
            String serviceName = request.getServiceName().trim();

            boolean exists = salonServiceRepository
                    .existsBySalonIdAndServiceNameIgnoreCaseAndServiceIdNot(
                            service.getSalonId(),
                            serviceName,
                            service.getServiceId()
                    );

            if (exists) {
                throw new DuplicateResourceException(
                        "Service name already in use. Please choose a different name."
                );
            }

            service.setServiceName(serviceName);
        }

        if (request.getDurationMinutes() != null) {
            service.setDurationMinutes(request.getDurationMinutes());
        }

        if (request.getBufferMinutes() != null) {
            service.setBufferMinutes(request.getBufferMinutes());
        }

        if (request.getPrice() != null) {
            service.setPrice(request.getPrice());
        }

        if (request.getIsActive() != null) {
            service.setIsActive(request.getIsActive());
        }

        SalonServiceDTO dto = new SalonServiceDTO();
        BeanUtils.copyProperties(salonServiceRepository.save(service), dto);

        return dto;
    }

    /**
     * Retrieves all services grouped by category.
     */
    // GET ALL SERVICES
    public List<CategoryResponse> getAllServices() {
        List<CategoryServiceDTO> services =
                salonServiceRepository.fetchCategoryAndServices();

        Map<String, Set<String>> grouped = services.stream()
                .collect(Collectors.groupingBy(
                        dto -> dto.getCategoryName().trim().toLowerCase(),
                        Collectors.mapping(
                                dto -> dto.getServiceName().trim().toLowerCase(),
                                Collectors.toSet()
                        )
                ));

        return grouped.entrySet().stream()
                .map(entry -> new CategoryResponse(
                        capitalize(entry.getKey()),
                        entry.getValue().stream()
                                .map(this::capitalize)
                                .toList()
                ))
                .toList();
    }

    /**
     * Fetches a single service by its ID.
     */
    // GET SERVICE BY ID
    public SalonService getServiceById(Long serviceId) {
        return salonServiceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
    }

    /**
     * Fetches services for a given salon and category.
     */
    // GET SERVICES BY CATEGORY AND SALON
    public List<SalonService> getServicesByCategoryAndSalon(Long salonId, Long categoryId) {
        salonRepository.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon not found"));

        categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        return salonServiceRepository.findBySalonIdAndCategoryId(salonId, categoryId);
    }

    /**
     * Fetches paginated services for a given salon.
     */
    // GET SERVICES BY SALON PAGINATION
    public Page<SalonService> getServicesBySalon(Long salonId, int pageNo, int pageSize) {
        salonRepository.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon not found"));

        Pageable pageable = PageRequest.of(pageNo, pageSize);

        return salonServiceRepository.findBySalonId(salonId, pageable);
    }

    /**
     * Helper method to capitalize each word in a string.
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty())
            return str;

        return Arrays.stream(str.split(" "))
                .map(word ->
                        word.substring(0, 1).toUpperCase() + word.substring(1)
                )
                .collect(Collectors.joining(" "));
    }
}