package org.backend.service;

import lombok.RequiredArgsConstructor;
import org.backend.dto.SalonDetailsDTO;
import org.backend.dto.SalonSearchWithSelectedServicesResponseDTO;
import org.backend.dto.ServiceDTO;
import org.backend.dto.common.PageResponse;
import org.backend.exception.BadRequestException;
import org.backend.exception.ResourceNotFoundException;
import org.backend.model.SalonImages;
import org.backend.model.SalonService;
import org.backend.projection.NearBySalonsProjection;
import org.backend.repository.CategoryRepository;
import org.backend.repository.SalonImagesRepository;
import org.backend.repository.SalonRepository;
import org.backend.repository.SalonServiceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service class for searching salons based on location and services.
 * Provides functionality to find nearby salons, paginate results, and search salons
 * that offer specific services within a geographical area.
 */
@Service
@RequiredArgsConstructor
public class SalonSearchService {

    private final SalonRepository salonRepository;
    private final SalonServiceRepository salonServiceRepository;
    private final CategoryRepository categoryRepository;
    private final SalonImagesRepository salonImageRepository;

    /**
     * Finds salons near a given location within a specified distance.
     */
    // FIND NEARBY SALONS
    public List<SalonDetailsDTO> findNearbySalons(double latitude, double longitude, double distance, String unit) {

        List<NearBySalonsProjection> nearbySalons = fetchNearbySalons(latitude, longitude, distance, unit, null);

        List<SalonDetailsDTO> salons = nearbySalons.stream().map(p -> {
            double finalDistance = ("M".equalsIgnoreCase(unit))
                    ? p.getDistanceKm() * 1000
                    : p.getDistanceKm();

            return SalonDetailsDTO.builder()
                    .salonId(p.getSalonId())
                    .partnerId(p.getPartnerId())
                    .salonName(p.getSalonName())
                    .latitude(p.getLatitude())
                    .longitude(p.getLongitude())
                    .addressLine1(p.getAddressLine1())
                    .addressLine2(p.getAddressLine2())
                    .landmark(p.getLandmark())
                    .city(p.getCity())
                    .state(p.getState())
                    .zipCode(p.getZipCode())
                    .country(p.getCountry())
                    .workingDays(p.getWorkingDays())
                    .workingHoursStart(p.getWorkingHoursStart())
                    .workingHoursEnd(p.getWorkingHoursEnd())
                    .distance(Math.round(finalDistance * 100.0) / 100.0)
                    .unit(unit == null ? "KM" : unit.toUpperCase())
                    .build();
        }).toList();

        List<Long> salonIds = salons.stream()
                .map(SalonDetailsDTO::getSalonId)
                .toList();

        if (salonIds.isEmpty())
            return salons;

        List<SalonImages> images = salonImageRepository.findFrontViewImages(salonIds);

        Map<Long, String> imageMap = images.stream()
                .collect(Collectors.toMap(
                        SalonImages::getSalonId,
                        SalonImages::getImageUrl,
                        (existing, replacement) -> existing
                ));

        return salons.stream().map(salon -> {
            salon.setSalonImage(imageMap.get(salon.getSalonId()));
            return salon;
        }).toList();
    }

    /**
     * Finds nearby salons with pagination support.
     */
    // FIND NEARBY SALONS WITH PAGINATION
    public PageResponse<SalonDetailsDTO> findNearbySalonsWithPagination(
            double latitude,
            double longitude,
            double distance,
            String unit,
            int page,
            int size) {

        if (page < 1) {
            throw new BadRequestException("Page number must be >= 1");
        }

        if (size < 1 || size >= 100) {
            throw new BadRequestException("Page size must be between 1 and 100");
        }

        double distanceKm = ("M".equalsIgnoreCase(unit)) ? distance / 1000.0 : distance;
        double latDelta = distanceKm / 111.0;
        double lonDelta = distanceKm / (111.0 * Math.cos(Math.toRadians(latitude)));
        double minLat = latitude - latDelta;
        double maxLat = latitude + latDelta;
        double minLon = longitude - lonDelta;
        double maxLon = longitude + lonDelta;

        Pageable pageable = PageRequest.of(page - 1, size);

        Page<NearBySalonsProjection> pageResult =
                salonRepository.findNearbySalonsWithPagination(
                        latitude, longitude,
                        minLat, maxLat,
                        minLon, maxLon,
                        distanceKm,
                        pageable
                );

        List<SalonDetailsDTO> salons = pageResult.getContent().stream().map(p -> {
            double finalDistance = ("M".equalsIgnoreCase(unit))
                    ? p.getDistanceKm() * 1000
                    : p.getDistanceKm();

            return SalonDetailsDTO.builder()
                    .salonId(p.getSalonId())
                    .partnerId(p.getPartnerId())
                    .salonName(p.getSalonName())
                    .latitude(p.getLatitude())
                    .longitude(p.getLongitude())
                    .addressLine1(p.getAddressLine1())
                    .addressLine2(p.getAddressLine2())
                    .landmark(p.getLandmark())
                    .city(p.getCity())
                    .state(p.getState())
                    .zipCode(p.getZipCode())
                    .country(p.getCountry())
                    .workingDays(p.getWorkingDays())
                    .workingHoursStart(p.getWorkingHoursStart())
                    .workingHoursEnd(p.getWorkingHoursEnd())
                    .distance(Math.round(finalDistance * 100.0) / 100.0)
                    .unit(unit == null ? "KM" : unit.toUpperCase())
                    .build();
        }).toList();

        List<Long> salonIds = salons.stream()
                .map(SalonDetailsDTO::getSalonId)
                .toList();

        if (!salonIds.isEmpty()) {
            List<SalonImages> images = salonImageRepository.findFrontViewImages(salonIds);

            Map<Long, String> imageMap = images.stream()
                    .collect(Collectors.toMap(
                            SalonImages::getSalonId,
                            SalonImages::getImageUrl,
                            (e, r) -> e
                    ));

            salons.forEach(salon ->
                    salon.setSalonImage(imageMap.get(salon.getSalonId()))
            );
        }

        return PageResponse.<SalonDetailsDTO>builder()
                .page(page)
                .size(size)
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .content(salons)
                .build();
    }

    /**
     * Finds salons that offer all the selected services within a geographical area.
     */
    // FIND SALONS WITH SELECTED SERVICES
    public List<SalonSearchWithSelectedServicesResponseDTO> findSalonsWithSelectedServices(
            double latitude,
            double longitude,
            double distance,
            String unit,
            List<String> selectedServices) {

        if (selectedServices == null ||
                selectedServices.isEmpty() ||
                selectedServices.stream().allMatch(s -> s == null || s.trim().isEmpty())) {
            throw new BadRequestException("At least one valid service must be selected.");
        }

        List<SalonDetailsDTO> nearbySalons = findNearbySalons(latitude, longitude, distance, unit);

        if (nearbySalons.isEmpty())
            return List.of();

        List<Long> salonIds = nearbySalons.stream()
                .map(SalonDetailsDTO::getSalonId)
                .toList();

        List<SalonService> services = salonServiceRepository.findBySalonIds(salonIds);

        Set<String> requiredServices = selectedServices.stream()
                .map(s -> s.toLowerCase().trim())
                .collect(Collectors.toSet());

        Map<Long, List<SalonService>> salonServiceMap = services.stream()
                .collect(Collectors.groupingBy(SalonService::getSalonId));

        return nearbySalons.stream()
                .map(salon -> {
                    List<SalonService> salonServices =
                            salonServiceMap.getOrDefault(salon.getSalonId(), List.of());

                    List<SalonService> matchedServices = salonServices.stream()
                            .filter(s -> requiredServices.contains(
                                    s.getServiceName().toLowerCase().trim()
                            ))
                            .toList();

                    if (matchedServices.size() != requiredServices.size()) {
                        return null;
                    }

                    List<ServiceDTO> serviceDTOs = matchedServices.stream()
                            .map(s -> new ServiceDTO(s.getServiceName(), s.getPrice()))
                            .toList();

                    BigDecimal totalPrice = matchedServices.stream()
                            .map(SalonService::getPrice)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return SalonSearchWithSelectedServicesResponseDTO.builder()
                            .salonId(salon.getSalonId())
                            .salonName(salon.getSalonName())
                            .salonImageUrl(salon.getSalonImage())
                            .distance(salon.getDistance())
                            .unit(salon.getUnit())
                            .totalPrice(totalPrice)
                            .serviceDetails(serviceDTOs)
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();
    }

    // Search nearby salons by search keyword it will give suggestions of salon name which are nearby to user location
    public List<SalonDetailsDTO> searchNearbySalonSuggestions(
            double latitude,
            double longitude,
            //double distance,
            //String unit,
            String keyword) {

        if (keyword == null || keyword.trim().isEmpty()) {
            throw new BadRequestException("Salon name is required");
        }

        //List<NearBySalonsProjection> rows = fetchNearbySalons(latitude, longitude, 0, "KM", keyword.trim());

        List<NearBySalonsProjection> rows = salonRepository.searchSalonsByKeyword(latitude, longitude, keyword.trim());

        Map<String, NearBySalonsProjection> uniqueMap = new LinkedHashMap<>();

        for (NearBySalonsProjection p : rows) {
            String key = p.getSalonName().toLowerCase().trim();
            uniqueMap.putIfAbsent(key, p);
        }

        return uniqueMap.values().stream()
                .map(p -> SalonDetailsDTO.builder()
                        .salonName(p.getSalonName())
                        .build())
                .toList();
    }

    // Get salon by name which is selected from suggestions and also nearby to user location
    public List<SalonDetailsDTO> getNearbySalonByName(
            String salonName, double latitude, double longitude) {

        if (salonName == null || salonName.trim().isEmpty()) {
            throw new BadRequestException("Salon name is required");
        }

        //List<NearBySalonsProjection> rows = fetchNearbySalons(latitude, longitude, 0, "KM", salonName.trim());

        List<NearBySalonsProjection> rows = salonRepository.searchSalonsByKeyword(latitude, longitude, salonName.trim());


        if (rows == null || rows.isEmpty()) {
            throw new ResourceNotFoundException("Salon not found");
        }

        String unit = "KM"; // Default unit

        return rows.stream().map(p -> {
            double finalDistance = ("M".equalsIgnoreCase(unit))
                    ? p.getDistanceKm() * 1000
                    : p.getDistanceKm();

            SalonDetailsDTO dto = SalonDetailsDTO.builder()
                    .salonId(p.getSalonId())
                    .partnerId(p.getPartnerId())
                    .salonName(p.getSalonName())
                    .latitude(p.getLatitude())
                    .longitude(p.getLongitude())
                    .addressLine1(p.getAddressLine1())
                    .addressLine2(p.getAddressLine2())
                    .landmark(p.getLandmark())
                    .city(p.getCity())
                    .state(p.getState())
                    .zipCode(p.getZipCode())
                    .country(p.getCountry())
                    .workingDays(p.getWorkingDays())
                    .workingHoursStart(p.getWorkingHoursStart())
                    .workingHoursEnd(p.getWorkingHoursEnd())
                    .distance(Math.round(finalDistance * 100.0) / 100.0)
                    .unit(unit == null ? "KM" : unit.toUpperCase())
                    .build();

            salonImageRepository.findFrontViewImage(p.getSalonId())
                    .ifPresent(img -> dto.setSalonImage(img.getImageUrl()));

            return dto;
        }).toList();
    }


    private List<NearBySalonsProjection> fetchNearbySalons(
            double latitude, double longitude,
            double distance, String unit, String keyword) {

        double distanceKm = ("M".equalsIgnoreCase(unit)) ? distance / 1000.0 : distance;
        double latDelta = distanceKm / 111.0;
        double lonDelta = distanceKm / (111.0 * Math.cos(Math.toRadians(latitude)));

        double minLat = latitude - latDelta;
        double maxLat = latitude + latDelta;

        double minLon = longitude - lonDelta;
        double maxLon = longitude + lonDelta;

        if (keyword != null && !keyword.trim().isEmpty()){
            return salonRepository.searchNearbySalons(
                    latitude, longitude, keyword.trim());
        }

        return salonRepository.findNearbySalons(
                latitude, longitude,
                minLat, maxLat, minLon, maxLon, distanceKm);
    }


    public List<SalonDetailsDTO> getPopularSalons(
            double latitude,
            double longitude,
            double distance,
            String unit,
            Integer size
    ) {

        int finalSize = (size == null) ? 20 : size;

        if (finalSize < 1 || finalSize > 20) {
            throw new BadRequestException("Size must be between 1 and 20");
        }

        String normalizedUnit = (unit == null || unit.isBlank())
                ? "KM"
                : unit.trim().toUpperCase();

        double distanceKm = normalizedUnit.equals("M")
                ? distance / 1000.0
                : distance;

        double latDelta = distanceKm / 111.0;
        double lonDelta = distanceKm /
                (111.0 * Math.cos(Math.toRadians(latitude)));

        double minLat = latitude - latDelta;
        double maxLat = latitude + latDelta;
        double minLon = longitude - lonDelta;
        double maxLon = longitude + lonDelta;

        List<NearBySalonsProjection> results =
                salonRepository.findPopularSalons(
                        latitude, longitude, minLat, maxLat,
                        minLon, maxLon, distanceKm, finalSize
                );

        if (results.isEmpty()) {
            return Collections.emptyList();
        }

        List<SalonDetailsDTO> salons = results.stream()
                .map(projection -> {

                    double finalDistance = normalizedUnit.equals("M")
                            ? projection.getDistanceKm() * 1000
                            : projection.getDistanceKm();

                    return SalonDetailsDTO.builder()
                            .salonId(projection.getSalonId())
                            .partnerId(projection.getPartnerId())
                            .salonName(projection.getSalonName())
                            .latitude(projection.getLatitude())
                            .longitude(projection.getLongitude())
                            .addressLine1(projection.getAddressLine1())
                            .addressLine2(projection.getAddressLine2())
                            .landmark(projection.getLandmark())
                            .city(projection.getCity())
                            .state(projection.getState())
                            .zipCode(projection.getZipCode())
                            .country(projection.getCountry())
                            .workingDays(projection.getWorkingDays())
                            .workingHoursStart(projection.getWorkingHoursStart())
                            .workingHoursEnd(projection.getWorkingHoursEnd())
                            .distance(Math.round(finalDistance * 100.0) / 100.0)
                            .unit(normalizedUnit)
                            .build();
                })
                .toList();

        List<Long> salonIds = salons.stream()
                .map(SalonDetailsDTO::getSalonId)
                .toList();

        List<SalonImages> images =
                salonImageRepository.findFrontViewImages(salonIds);

        Map<Long, String> imageMap = images.stream()
                .collect(Collectors.toMap(
                        SalonImages::getSalonId,
                        SalonImages::getImageUrl,
                        (existing, duplicate) -> existing
                ));

        salons.forEach(salon ->
                salon.setSalonImage(imageMap.get(salon.getSalonId()))
        );

        return salons;
    }
}