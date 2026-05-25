package org.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.backend.dto.PackageResponseDTO;
import org.backend.dto.UpdatePackageRequestDTO;
import org.backend.dto.common.ApiResponseDTO;
import org.backend.dto.packagee.CreatePackageRequestDTO;
import org.backend.service.PackageManagementService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/packages")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class PackageController {

    private final PackageManagementService packageManagementService;

    /*
     * CREATE PACKAGE
     */
    @PostMapping
    public ApiResponseDTO<PackageResponseDTO> createPackage(
            @Valid @RequestBody CreatePackageRequestDTO req
    ) {

        PackageResponseDTO response =
                packageManagementService.createPackage(req);

        return ApiResponseDTO.<PackageResponseDTO>builder()
                .status(true)
                .message("Package created successfully")
                .data(response)
                .build();
    }

    /*
     * UPDATE PACKAGE
     */
    @PutMapping("/salon/{salonId}/{packageId}")
    public ApiResponseDTO<PackageResponseDTO> updatePackage(
            @PathVariable Long salonId,
            @PathVariable Long packageId,
            @RequestBody UpdatePackageRequestDTO req
    ) {

        PackageResponseDTO response =
                packageManagementService.updatePackage(
                        salonId,
                        packageId,
                        req
                );

        return ApiResponseDTO.<PackageResponseDTO>builder()
                .status(true)
                .message("Package updated successfully")
                .data(response)
                .build();
    }

    /*
     * GET PACKAGE BY ID
     */
    @GetMapping("/{packageId}")
    public ApiResponseDTO<PackageResponseDTO> getPackageById(
            @PathVariable Long packageId
    ) {

        PackageResponseDTO response =
                packageManagementService.getPackageById(packageId);

        return ApiResponseDTO.<PackageResponseDTO>builder()
                .status(true)
                .message("Package fetched successfully")
                .data(response)
                .build();
    }

    /*
     * GET ALL PACKAGES
     */
    @GetMapping
    public ApiResponseDTO<List<PackageResponseDTO>> getAllPackages() {

        List<PackageResponseDTO> response =
                packageManagementService.getAllPackages();

        return ApiResponseDTO.<List<PackageResponseDTO>>builder()
                .status(true)
                .message("Packages fetched successfully")
                .data(response)
                .build();
    }

    /*
     * GET PACKAGES BY SALON ID
     */
    @GetMapping("/salon/{salonId}")
    public ApiResponseDTO<List<PackageResponseDTO>> getPackagesBySalonId(
            @PathVariable Long salonId
    ) {

        List<PackageResponseDTO> response =
                packageManagementService.getPackagesBySalonId(salonId);

        return ApiResponseDTO.<List<PackageResponseDTO>>builder()
                .status(true)
                .message("Salon packages fetched successfully")
                .data(response)
                .build();
    }

    /*
     * DELETE PACKAGE (SOFT DELETE)
     */
    @DeleteMapping("/{packageId}")
    public ApiResponseDTO<String> deletePackage(
            @PathVariable Long packageId,
            @RequestParam Long salonId
    ) {

        packageManagementService.deletePackage(packageId, salonId);

        return ApiResponseDTO.<String>builder()
                .status(true)
                .message("Package deleted successfully")
                .data("SUCCESS")
                .build();
    }
}