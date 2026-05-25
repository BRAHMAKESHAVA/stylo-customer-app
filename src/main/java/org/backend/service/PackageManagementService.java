package org.backend.service;

import lombok.RequiredArgsConstructor;
import org.backend.dto.PackageResponseDTO;
import org.backend.dto.ServiceInfoDTO;
import org.backend.dto.UpdatePackageRequestDTO;
import org.backend.dto.packagee.CreatePackageRequestDTO;
import org.backend.exception.BadRequestException;
import org.backend.exception.ResourceNotFoundException;
import org.backend.model.Package;
import org.backend.model.PackageService;
import org.backend.model.SalonDetails;
import org.backend.model.SalonService;
import org.backend.repository.PackageRepository;
import org.backend.repository.PackageServiceRepository;
import org.backend.repository.SalonRepository;
import org.backend.repository.SalonServiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PackageManagementService {

    private final PackageRepository packageRepository;
    private final PackageServiceRepository packageServiceRepository;
    private final SalonRepository salonRepository;
    private final SalonServiceRepository serviceRepository;

    /*
     * CREATE PACKAGE
     */
    @Transactional
    public PackageResponseDTO createPackage(CreatePackageRequestDTO req) {

        salonRepository.findById(req.getSalonId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Salon not found"));

        String normalizedName = normalize(req.getPackageName());

        List<Package> existingPackages =
                packageRepository.findBySalonId(req.getSalonId());

        boolean duplicateExists = existingPackages.stream()
                .anyMatch(pkg ->
                        normalize(pkg.getPackageName())
                                .equals(normalizedName));

        if (duplicateExists) {
            throw new BadRequestException(
                    "Package name already exists for this salon");
        }

        List<SalonService> services =
                serviceRepository.findAllById(req.getServiceIds());

        if (services.isEmpty()) {
            throw new ResourceNotFoundException("Services not found");
        }

        if (services.size() != req.getServiceIds().size()) {
            throw new BadRequestException("Invalid services selected");
        }

        boolean invalidService = services.stream()
                .anyMatch(service ->
                        !service.getSalonId().equals(req.getSalonId())
                                || !Boolean.TRUE.equals(service.getIsActive()));

        if (invalidService) {
            throw new BadRequestException(
                    "Selected services do not belong to this salon");
        }

        Package pkg = Package.builder()
                .salonId(req.getSalonId())
                .packageName(req.getPackageName().trim())
                .description(req.getDescription())
                .packagePrice(req.getPackagePrice())
                .isActive(true)
                .build();

        Package savedPackage = packageRepository.save(pkg);

        List<PackageService> mappings = req.getServiceIds()
                .stream()
                .map(serviceId ->
                        PackageService.builder()
                                .packageId(savedPackage.getPackageId())
                                .serviceId(serviceId)
                                .build()
                )
                .collect(Collectors.toList());

        packageServiceRepository.saveAll(mappings);

        return buildResponse(savedPackage);
    }

    /*
     * UPDATE PACKAGE
     */
    @Transactional
    public PackageResponseDTO updatePackage(
            Long salonId,
            Long packageId,
            UpdatePackageRequestDTO req
    ) {

        Package pkg = packageRepository
                .findByPackageIdAndSalonId(packageId, salonId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Package not found for this salon"));

        /*
         * PACKAGE NAME UPDATE
         */
        if (req.getPackageName() != null &&
                !req.getPackageName().trim().isEmpty()) {

            String newName = normalize(req.getPackageName());

            List<Package> salonPackages =
                    packageRepository.findBySalonId(salonId);

            boolean duplicateExists = salonPackages.stream()
                    .filter(existing ->
                            !existing.getPackageId().equals(packageId))
                    .anyMatch(existing ->
                            Boolean.TRUE.equals(existing.getIsActive()) &&
                                    normalize(existing.getPackageName())
                                            .equals(newName));

            if (duplicateExists) {
                throw new BadRequestException(
                        "Package name already exists for this salon");
            }

            pkg.setPackageName(req.getPackageName().trim());
        }

        /*
         * DESCRIPTION UPDATE
         */
        if (req.getDescription() != null) {
            pkg.setDescription(req.getDescription().trim());
        }

        /*
         * PRICE UPDATE
         */
        if (req.getPackagePrice() != null) {

            if (req.getPackagePrice().signum() <= 0) {
                throw new BadRequestException(
                        "Package price must be greater than zero");
            }

            pkg.setPackagePrice(req.getPackagePrice());
        }

        /*
         * ACTIVE FLAG UPDATE
         */
        if (req.getIsActive() != null) {
            pkg.setIsActive(req.getIsActive());
        }

        /*
         * SERVICE UPDATE
         */
        if (req.getServiceIds() != null &&
                !req.getServiceIds().isEmpty()) {

            List<SalonService> services =
                    serviceRepository.findAllById(req.getServiceIds());

            if (services.size() != req.getServiceIds().size()) {
                throw new BadRequestException("Invalid services selected");
            }

            boolean invalidService = services.stream()
                    .anyMatch(service ->
                            !service.getSalonId().equals(salonId)
                                    || !Boolean.TRUE.equals(service.getIsActive()));

            if (invalidService) {
                throw new BadRequestException(
                        "Selected services do not belong to this salon");
            }

            packageServiceRepository.deleteByPackageId(packageId);

            List<PackageService> mappings = req.getServiceIds()
                    .stream()
                    .map(serviceId ->
                            PackageService.builder()
                                    .packageId(packageId)
                                    .serviceId(serviceId)
                                    .build()
                    )
                    .toList();

            packageServiceRepository.saveAll(mappings);
        }

        Package saved = packageRepository.save(pkg);

        return buildResponse(saved);
    }

    /*
     * GET PACKAGE BY ID
     */
    public PackageResponseDTO getPackageById(Long packageId) {

        Package pkg = packageRepository.findById(packageId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Package not found"));

        if (!Boolean.TRUE.equals(pkg.getIsActive())) {
            throw new ResourceNotFoundException("Package not found");
        }

        return buildResponse(pkg);
    }

    /*
     * GET ALL PACKAGES
     */
    public List<PackageResponseDTO> getAllPackages() {

        List<Package> packages = packageRepository.findByIsActiveTrue();

        return packages.stream()
                .map(this::buildResponse)
                .collect(Collectors.toList());
    }

    /*
     * GET PACKAGES BY SALON
     */
    public List<PackageResponseDTO> getPackagesBySalonId(Long salonId) {

        salonRepository.findById(salonId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Salon not found"));

        List<Package> packages =
                packageRepository.findBySalonIdAndIsActiveTrue(salonId);

        return packages.stream()
                .map(this::buildResponse)
                .collect(Collectors.toList());
    }

    /*
     * DELETE PACKAGE (SOFT DELETE)
     */
    @Transactional
    public void deletePackage(Long packageId, Long salonId) {

        Package pkg = packageRepository.findById(packageId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Package not found"));

        if (!pkg.getSalonId().equals(salonId)) {
            throw new BadRequestException(
                    "Package does not belong to the given salon");
        }

        pkg.setIsActive(false);

        packageRepository.save(pkg);
    }

    /*
     * RESPONSE BUILDER
     */
    private PackageResponseDTO buildResponse(Package pkg) {

        List<PackageService> mappings =
                packageServiceRepository.findByPackageId(pkg.getPackageId());

        List<Long> serviceIds = mappings.stream()
                .map(PackageService::getServiceId)
                .collect(Collectors.toList());

        List<ServiceInfoDTO> serviceInfos =
                serviceRepository.findAllById(serviceIds)
                        .stream()
                        .map(service ->
                                ServiceInfoDTO.builder()
                                        .serviceId(service.getServiceId())
                                        .serviceName(service.getServiceName())
                                        .price(service.getPrice())
                                        .durationMinutes(service.getDurationMinutes())
                                        .build()
                        )
                        .collect(Collectors.toList());

        return PackageResponseDTO.builder()
                .packageId(pkg.getPackageId())
                .salonId(pkg.getSalonId())
                .packageName(pkg.getPackageName())
                .description(pkg.getDescription())
                .packagePrice(pkg.getPackagePrice())
                .isActive(pkg.getIsActive())
                .services(serviceInfos)
                .build();
    }

    /*
     * NORMALIZE STRING
     */
    private String normalize(String input) {
        return input.trim()
                .replaceAll("\\s+", " ")
                .toLowerCase();
    }
}