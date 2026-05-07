package org.backend.service;

import lombok.RequiredArgsConstructor;
import org.backend.dto.SalonResourceDTO;
import org.backend.dto.UpdateSalonResourceRequestDTO;
import org.backend.exception.BadRequestException;
import org.backend.exception.DuplicateResourceException;
import org.backend.exception.ResourceNotFoundException;
import org.backend.model.SalonResource;
import org.backend.repository.SalonRepository;
import org.backend.repository.SalonResourceRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SalonResourceService {

    private final SalonResourceRepository resourceRepository;
    private final SalonRepository salonRepository;

    /**
     * Create a new resource for a salon.
     */
    // CREATE RESOURCE
    public SalonResourceDTO createResource(SalonResourceDTO request) {

        salonRepository.findById(request.getSalonId())
                .orElseThrow(() -> new ResourceNotFoundException("Salon not found"));

        if (resourceRepository.existsBySalonId(request.getSalonId())) {
            throw new DuplicateResourceException(
                    "Resource already exists for this salon. Please update the existing one."
            );
        }

        SalonResource resource = new SalonResource();
        BeanUtils.copyProperties(request, resource);

        SalonResourceDTO response = new SalonResourceDTO();
        BeanUtils.copyProperties(resourceRepository.save(resource), response);

        return response;
    }

    /**
     * Retrieve a resource by salon ID.
     */
    // GET RESOURCE BY SALON
    public SalonResource getResourceBySalonId(Long salonId) {
        salonRepository.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon not found"));

        return resourceRepository.findBySalonId(salonId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Resource not found for salon")
                );
    }

    /**
     * Update an existing resource.
     */
    // UPDATE RESOURCE
    public SalonResourceDTO updateResource(Long resourceId, UpdateSalonResourceRequestDTO request) {

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

        SalonResourceDTO response = new SalonResourceDTO();
        BeanUtils.copyProperties(resourceRepository.save(resource), response);

        return response;
    }

    /**
     * Delete a resource by salon and resource ID.
     */
    // DELETE RESOURCE
    public void deleteResource(Long salonId, Long resourceId) {
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