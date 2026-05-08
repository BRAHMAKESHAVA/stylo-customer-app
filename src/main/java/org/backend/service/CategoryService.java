package org.backend.service;

import lombok.RequiredArgsConstructor;
import org.backend.dto.ServiceCategoryDTO;
import org.backend.dto.UpdateServiceCategoryRequestDTO;
import org.backend.dto.common.PageResponse;
import org.backend.exception.BadRequestException;
import org.backend.exception.DuplicateResourceException;
import org.backend.exception.ResourceNotFoundException;
import org.backend.model.ServiceCategory;
import org.backend.repository.CategoryRepository;
import org.backend.repository.SalonRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service class for managing service categories.
 * Provides business logic for creating, updating, deleting, and retrieving service categories
 * associated with salons, including validation for uniqueness and existence.
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final SalonRepository salonRepository;

    /**
     * Creates a new category for a salon.
     * Validates salon existence and ensures category name uniqueness within the salon.
     *
     * @param category the category details to create
     * @return the created ServiceCategory
     * @throws ResourceNotFoundException  if salon not found
     * @throws DuplicateResourceException if category name already exists for the salon
     */
    // CREATE CATEGORY
    public ServiceCategoryDTO createCategory(ServiceCategoryDTO category) {

        salonRepository.findById(category.getSalonId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Salon not found with id: " + category.getSalonId()
                        )
                );

        String categoryName = category.getCategoryName().trim();

        if (categoryName.isEmpty()) {
            throw new BadRequestException(
                    "Category name cannot be empty. Please provide a valid name."
            );
        }

        boolean exists = categoryRepository
                .existsBySalonIdAndCategoryNameIgnoreCase(
                        category.getSalonId(),
                        categoryName
                );

        if (exists)
            throw new DuplicateResourceException(
                    "Category '" + categoryName + "' already exists for this salon"
            );

        category.setCategoryName(categoryName);

        if (category.getIsActive() == null)
            category.setIsActive(true);

        ServiceCategory newCateory = new ServiceCategory();
        BeanUtils.copyProperties(category, newCateory);

        ServiceCategoryDTO response = new ServiceCategoryDTO();
        BeanUtils.copyProperties(categoryRepository.save(newCateory), response);

        return response;
    }

    /**
     * Updates an existing category.
     * Allows updating category name and active status, with validation for uniqueness.
     *
     * @param id      the ID of the category to update
     * @param request the update request containing new details
     * @return the updated ServiceCategory
     * @throws BadRequestException        if salon ID is missing or category name is empty
     * @throws ResourceNotFoundException  if category not found for the salon
     * @throws DuplicateResourceException if new category name already exists
     */
    // UPDATE CATEGORY
    public ServiceCategoryDTO updateCategory(Long id, UpdateServiceCategoryRequestDTO request) {
        if (request.getSalonId() == null) {
            throw new BadRequestException(
                    "Salon ID is required to update the category."
            );
        }

        ServiceCategory category = categoryRepository
                .findByCategoryIdAndSalonId(id, request.getSalonId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Category with ID:" + id +
                                        " was not found for salon:" + request.getSalonId()
                        )
                );

        if (request.getCategoryName() != null) {
            String categoryName = request.getCategoryName().trim();

            if (categoryName.isEmpty())
                throw new BadRequestException(
                        "Category name cannot be empty. Please provide a valid name."
                );

            boolean exists = categoryRepository
                    .existsBySalonIdAndCategoryNameIgnoreCaseAndCategoryIdNot(
                            category.getSalonId(),
                            categoryName,
                            category.getCategoryId()
                    );

            if (exists)
                throw new DuplicateResourceException(
                        "Category name already in use. Please use a different name."
                );

            if (category.getIsActive() == null)
                category.setIsActive(true);

            category.setCategoryName(categoryName);
        }

        if (request.getIsActive() != null)
            category.setIsActive(request.getIsActive());

        ServiceCategoryDTO response = new ServiceCategoryDTO();
        BeanUtils.copyProperties(categoryRepository.save(category), response);

        return response;
    }

    /**
     * Deletes a category by its ID.
     * Ensures the category exists and belongs to the specified salon before deletion.
     *
     * @param salonId    the ID of the salon
     * @param categoryId the ID of the category to delete
     * @throws ResourceNotFoundException if category not found for the salon
     */
    // DELETE CATEGORY
    public void deleteCategory(Long salonId, Long categoryId) {
        ServiceCategory category = categoryRepository
                .findByCategoryIdAndSalonId(categoryId, salonId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Category not found for this salon"
                        )
                );

        //Soft delete
        category.setIsActive(false);
        categoryRepository.save(category);
    }

    /**
     * Retrieves all categories.
     *
     * @return list of all ServiceCategory
     */
    // GET ALL CATEGORIES
    public PageResponse<ServiceCategory> getAllCategories(int page, int size) {

        if (page < 1) {
            throw new BadRequestException("Page number must be >= 1");
        }

        if (size < 1 || size > 100) {
            throw new BadRequestException("Page size must be between 1 and 100");
        }

        // Convert to 0-based index
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("categoryId").descending());

        Page<ServiceCategory> pageData = categoryRepository.findAll(pageable);

        return PageResponse.<ServiceCategory>builder()
                .page(page)
                .size(size)
                .totalElements(pageData.getTotalElements())
                .totalPages(pageData.getTotalPages())
                .last(pageData.isLast())
                .content(pageData.getContent())
                .build();
    }

    /**
     * Retrieves all categories belonging to a specific salon.
     *
     * @param salonId the ID of the salon
     * @return list of ServiceCategory for the salon
     * @throws ResourceNotFoundException if salon not found
     */
    // GET CATEGORIES BY SALON
    public List<ServiceCategory> getCategoriesBySalon(Long salonId) {
        salonRepository.findById(salonId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Salon not found with id: " + salonId
                        )
                );

        return categoryRepository.findBySalonId(salonId);
    }
}