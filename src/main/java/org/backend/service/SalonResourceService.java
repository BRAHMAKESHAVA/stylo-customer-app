package org.backend.service;

import lombok.RequiredArgsConstructor;
import org.backend.dto.request.CreateSalonResourceRequest;
import org.backend.dto.request.UpdateSalonResourceRequest;
import org.backend.dto.response.SalonResourceResponse;
import org.backend.enums.Role;
import org.backend.exception.BadRequestException;
import org.backend.exception.DuplicateResourceException;
import org.backend.exception.ResourceNotFoundException;
import org.backend.model.PartnerDetails;
import org.backend.model.SalonDetails;
import org.backend.model.SalonResource;
import org.backend.model.Users;
import org.backend.repository.PartnerRepository;
import org.backend.repository.SalonRepository;
import org.backend.repository.SalonResourceRepository;
import org.backend.repository.UserRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SalonResourceService {

    private final SalonResourceRepository resourceRepository;
    private final SalonRepository salonRepository;
    private final AuthService authService;
    /**
     * Create a new resource for a salon.
     */
    // CREATE RESOURCE
    public SalonResourceResponse createResource(CreateSalonResourceRequest request) {

        // Validate salon ownership/access before proceeding with resource creation
        authService.validateSalonAccess(request.getSalonId());

        if (resourceRepository.existsBySalonId(request.getSalonId())) {
            throw new DuplicateResourceException(
                    "Resource already exists for this salon. Please update the existing one."
            );
        }

        SalonResource resource = new SalonResource();
        BeanUtils.copyProperties(request, resource);

        SalonResourceResponse response = new SalonResourceResponse();
        BeanUtils.copyProperties(resourceRepository.save(resource), response);

        return response;
    }

    /**
     * Retrieve a resource by salon ID.
     */
    // GET RESOURCE BY SALON
    public SalonResourceResponse getResourceBySalonId(Long salonId) {
        salonRepository.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon not found"));

        SalonResource resource = resourceRepository.findBySalonId(salonId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Resource not found for salon")
                );

        SalonResourceResponse response = new SalonResourceResponse();
        BeanUtils.copyProperties(resource, response);

        return response;
    }

    /**
     * Update an existing resource.
     */
    // UPDATE RESOURCE
    public SalonResourceResponse updateResource(Long resourceId, UpdateSalonResourceRequest request) {

        // Validate salon ownership/access before proceeding with resource update
        authService.validateSalonAccess(request.getSalonId());

        if (request.getSalonId() == null) {
            throw new BadRequestException(
                    "Salon ID is required to update the resource."
            );
        }

        SalonResource resource = resourceRepository
                .findByIdAndSalonId(resourceId, request.getSalonId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Resource with ID:" + resourceId +
                                        " was not found for salon:" + request.getSalonId()
                        )
                );

        if (request.getResourceCount() != null) {
            if (request.getResourceCount() < 1)
                throw new BadRequestException("Resource count must be >= 1");

            resource.setResourceCount(request.getResourceCount());
        }

        SalonResourceResponse response = new SalonResourceResponse();
        BeanUtils.copyProperties(resourceRepository.save(resource), response);

        return response;
    }

    /**
     * Delete a resource by salon and resource ID.
     */
    // DELETE RESOURCE
    public void deleteResource(Long salonId, Long resourceId) {

        // Validate salon ownership/access before proceeding with resource deletion
        authService.validateSalonAccess(salonId);

        resourceRepository.findByIdAndSalonId(resourceId, salonId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Resource not found for salonId: " + salonId +
                                        " and resourceId: " + resourceId
                        )
                );

        resourceRepository.deleteById(resourceId);
    }
}