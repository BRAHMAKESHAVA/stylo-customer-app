package org.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.backend.dto.PackageResponseDTO;
import org.backend.dto.ServiceInfoDTO;
import org.backend.dto.request.CreatePackageRequestDTO;
import org.backend.dto.request.UpdatePackageRequestDTO;
import org.backend.dto.response.PackageRecommendationDTO;
import org.backend.exception.BadRequestException;
import org.backend.exception.ResourceNotFoundException;
import org.backend.model.Package;
import org.backend.model.PackageService;
import org.backend.model.SalonService;
import org.backend.repository.PackageRepository;
import org.backend.repository.PackageServiceRepository;
import org.backend.repository.SalonRepository;
import org.backend.repository.SalonServiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PackageManagementService {

    private final PackageRepository packageRepository;
    private final PackageServiceRepository packageServiceRepository;
    private final SalonRepository salonRepository;
    private final SalonServiceRepository serviceRepository;

    private BigDecimal maxPriceDifference = BigDecimal.valueOf(250.00);

    // CREATE PACKAGE
    @Transactional
    public PackageResponseDTO createPackage(CreatePackageRequestDTO request) {
        salonRepository.findById(request.getSalonId())
                .orElseThrow(() -> new ResourceNotFoundException("Salon not found"));

        String normalizedName = normalize(request.getPackageName());
        boolean duplicateExists = packageRepository.findBySalonId(request.getSalonId())
                .stream()
                .anyMatch(pkg -> normalize(pkg.getPackageName()).equals(normalizedName));

        if (duplicateExists) {
            throw new BadRequestException("Package name already exists for this salon");
        }

        List<SalonService> services = serviceRepository.findAllById(request.getServiceIds());
        if (services.isEmpty() || services.size() != request.getServiceIds().size()) {
            throw new BadRequestException("Invalid services selected");
        }

        boolean invalidService = services.stream()
                .anyMatch(service -> !service.getSalonId().equals(request.getSalonId())
                        || !Boolean.TRUE.equals(service.getIsActive()));
        if (invalidService) {
            throw new BadRequestException("Selected services do not belong to this salon");
        }

        Package pkg = Package.builder()
                .salonId(request.getSalonId())
                .packageName(request.getPackageName().trim())
                .description(request.getDescription())
                .packagePrice(request.getPackagePrice())
                .isActive(true)
                .build();

        Package savedPackage = packageRepository.save(pkg);

        List<PackageService> mappings = request.getServiceIds().stream()
                .map(serviceId -> PackageService.builder()
                        .packageId(savedPackage.getPackageId())
                        .serviceId(serviceId)
                        .build())
                .collect(Collectors.toList());

        packageServiceRepository.saveAll(mappings);

        return buildResponse(savedPackage);
    }

    // UPDATE PACKAGE
    @Transactional
    public PackageResponseDTO updatePackage(Long salonId, Long packageId, UpdatePackageRequestDTO request) {
        Package pkg = packageRepository.findByPackageIdAndSalonId(packageId, salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found for this salon"));

        if (request.getPackageName() != null && !request.getPackageName().trim().isEmpty()) {
            String newName = normalize(request.getPackageName());
            boolean duplicateExists = packageRepository.findBySalonId(salonId).stream()
                    .filter(existing -> !existing.getPackageId().equals(packageId))
                    .anyMatch(existing -> Boolean.TRUE.equals(existing.getIsActive())
                            && normalize(existing.getPackageName()).equals(newName));

            if (duplicateExists) {
                throw new BadRequestException("Package name already exists for this salon");
            }
            pkg.setPackageName(request.getPackageName().trim());
        }

        if (request.getDescription() != null) {
            pkg.setDescription(request.getDescription().trim());
        }

        if (request.getPackagePrice() != null) {
            if (request.getPackagePrice().signum() <= 0) {
                throw new BadRequestException("Package price must be greater than zero");
            }
            pkg.setPackagePrice(request.getPackagePrice());
        }

        if (request.getIsActive() != null) {
            pkg.setIsActive(request.getIsActive());
        }

        if (request.getServiceIds() != null && !request.getServiceIds().isEmpty()) {
            List<SalonService> services = serviceRepository.findAllById(request.getServiceIds());
            if (services.size() != request.getServiceIds().size()) {
                throw new BadRequestException("Invalid services selected");
            }

            boolean invalidService = services.stream()
                    .anyMatch(service -> !service.getSalonId().equals(salonId)
                            || !Boolean.TRUE.equals(service.getIsActive()));
            if (invalidService) {
                throw new BadRequestException("Selected services do not belong to this salon");
            }

            packageServiceRepository.deleteByPackageId(packageId);

            List<PackageService> mappings = request.getServiceIds().stream()
                    .map(serviceId -> PackageService.builder()
                            .packageId(packageId)
                            .serviceId(serviceId)
                            .build())
                    .toList();

            packageServiceRepository.saveAll(mappings);
        }

        Package saved = packageRepository.save(pkg);
        return buildResponse(saved);
    }

    // GET PACKAGE BY ID
    public PackageResponseDTO getPackageById(Long packageId) {
        Package pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found"));

        if (!Boolean.TRUE.equals(pkg.getIsActive())) {
            throw new ResourceNotFoundException("Package not found");
        }

        return buildResponse(pkg);
    }

    // GET ALL PACKAGES
    public List<PackageResponseDTO> getAllPackages() {
        return packageRepository.findByIsActiveTrue()
                .stream()
                .map(this::buildResponse)
                .collect(Collectors.toList());
    }

    // GET PACKAGES BY SALON
    public List<PackageResponseDTO> getPackagesBySalonId(Long salonId) {
        salonRepository.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon not found"));

        return packageRepository.findBySalonIdAndIsActiveTrue(salonId)
                .stream()
                .map(this::buildResponse)
                .collect(Collectors.toList());
    }

    // DELETE PACKAGE (SOFT DELETE)
    @Transactional
    public void deletePackage(Long packageId, Long salonId) {
        Package pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found"));

        if (!pkg.getSalonId().equals(salonId)) {
            throw new BadRequestException("Package does not belong to the given salon");
        }

        pkg.setIsActive(false);
        packageRepository.save(pkg);
    }

    // RESPONSE BUILDER
    private PackageResponseDTO buildResponse(Package pkg) {
        List<Long> serviceIds = packageServiceRepository.findByPackageId(pkg.getPackageId())
                .stream()
                .map(PackageService::getServiceId)
                .toList();

        List<ServiceInfoDTO> serviceInfos = serviceRepository.findAllById(serviceIds)
                .stream()
                .map(service -> ServiceInfoDTO.builder()
                        .serviceId(service.getServiceId())
                        .serviceName(service.getServiceName())
                        .price(service.getPrice())
                        .durationMinutes(service.getDurationMinutes())
                        .build())
                .toList();

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

    // PACKAGE RECOMMENDATION
    @Transactional(readOnly = true)
    public PackageRecommendationDTO getRecommendedPackage(Long salonId, Long selectedPackageId) {
        log.debug("Fetching recommendation for salonId={} and packageId={}", salonId, selectedPackageId);

        Package selectedPackage = packageRepository
                .findByPackageIdAndSalonIdAndIsActiveTrue(selectedPackageId, salonId)
                .orElseThrow(() -> new BadRequestException("Selected package not found"));

        List<Package> allPackages = packageRepository.findBySalonIdAndIsActiveTrue(salonId);
        if (allPackages.size() <= 1) {
            return null;
        }

        // Collect package IDs
        List<Long> packageIds = allPackages.stream()
                .map(Package::getPackageId)
                .toList();

        // Map package -> services
        Map<Long, Set<Long>> packageServiceMap = packageServiceRepository.findByPackageIdIn(packageIds).stream()
                .collect(Collectors.groupingBy(
                        PackageService::getPackageId,
                        Collectors.mapping(PackageService::getServiceId, Collectors.toSet())
                ));

        Set<Long> selectedServices = packageServiceMap.getOrDefault(selectedPackageId, Collections.emptySet());
        if (selectedServices.isEmpty()) {
            return null;
        }

        Package bestPackage = null;
        Set<Long> bestExtraServices = Collections.emptySet();
        BigDecimal bestPriceDifference = BigDecimal.ZERO;
        double bestScore = 0.0;

        for (Package candidate : allPackages) {
            if (candidate.getPackageId().equals(selectedPackageId)) continue;

            Set<Long> candidateServices = packageServiceMap.getOrDefault(candidate.getPackageId(), Collections.emptySet());
            if (!candidateServices.containsAll(selectedServices)) continue;

            Set<Long> extraServices = new HashSet<>(candidateServices);
            extraServices.removeAll(selectedServices);
            if (extraServices.isEmpty()) continue;

            BigDecimal priceDifference = candidate.getPackagePrice().subtract(selectedPackage.getPackagePrice());
            if (priceDifference.signum() <= 0 || priceDifference.compareTo(maxPriceDifference) > 0) continue;

            double score = extraServices.size() / priceDifference.doubleValue();
            if (score > bestScore) {
                bestScore = score;
                bestPackage = candidate;
                bestExtraServices = extraServices;
                bestPriceDifference = priceDifference;
            }
        }

        if (bestPackage == null) {
            return null;
        }

        List<SalonService> extraSalonServices = serviceRepository.findAllById(bestExtraServices);

        return PackageRecommendationDTO.builder()
                .currentPackageId(selectedPackage.getPackageId())
                .currentPackageName(selectedPackage.getPackageName())
                .currentPackagePrice(selectedPackage.getPackagePrice())
                .suggestedPackageId(bestPackage.getPackageId())
                .suggestedPackageName(bestPackage.getPackageName())
                .suggestedPackagePrice(bestPackage.getPackagePrice())
                .priceDifference(bestPriceDifference)
                .additionalServiceCount(extraSalonServices.size())
                .additionalServices(extraSalonServices.stream()
                        .map(SalonService::getServiceName)
                        .sorted()
                        .toList())
                .build();
    }

    // NORMALIZE STRING
    private String normalize(String input) {
        return input.trim().replaceAll("\\s+", " ").toLowerCase();
    }
}
