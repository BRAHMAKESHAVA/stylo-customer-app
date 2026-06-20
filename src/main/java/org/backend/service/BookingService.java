package org.backend.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.backend.dto.PackageResponseDTO;
import org.backend.dto.ServiceInfoDTO;
import org.backend.dto.booking.BookingRequestDTO;
import org.backend.dto.booking.BookingResponseDTO;
import org.backend.dto.request.PriceSummaryRequestDTO;
import org.backend.dto.response.SlotResponseDTO;
import org.backend.enums.*;
import org.backend.exception.BadRequestException;
import org.backend.model.*;
import org.backend.model.Package;
import org.backend.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepo;
    private final BookingServiceRepository bsRepo;
    private final SalonServiceRepository serviceRepo;
    private final PaymentRepository paymentRepo;
    private final SalonRepository salonRepo;
    private final PackageServiceRepository packageServiceRepository;
    private final PackageRepository packageRepository;
    private final SalonResourceRepository salonResourceRepo;
    private final AuthService authService;
    private final PartnerWebSocketService partnerWebSocketService;

    @Value("${app.booking.payment-hold-minutes}")
    private int paymentHoldMinutes;

    @Value("${app.booking.slot-interval-minutes}")
    private int slotIntervalMinutes;

    /**
     * CREATE BOOKING
     * paymentMode is no longer required here.
     * Booking is always created as PAYMENT_PENDING.
     * Payment record is created later when user selects payment mode on payment screen.
     */
    @Transactional
    public BookingResponseDTO createBooking(BookingRequestDTO bookingReq) {
        // Validate that the logged-in customer is authorized to access this customer record
        authService.validateCustomerAccess(bookingReq.getCustomerId());

        validateRequest(bookingReq);

        // idempotency check — prevent duplicate PAYMENT_PENDING bookings for same slot
        Optional<Booking> existing = bookingRepo
                .findFirstByCustomerIdAndSalonIdAndStartTimeAndStatus(
                        bookingReq.getCustomerId(),
                        bookingReq.getSalonId(),
                        bookingReq.getStartTime(),
                        BookingStatus.PAYMENT_PENDING.name()
                );

        if (existing.isPresent()) {
            Booking existingBooking = existing.get();
            if (existingBooking.getCreatedDate()
                    .isAfter(LocalDateTime.now().minusMinutes(paymentHoldMinutes))) {
                return buildResponse(existingBooking);
            }
        }

        BigDecimal grossAmount = BigDecimal.ZERO;
        Long totalDuration = 0L;
        List<BookingServiceEntity> bookingServices = new ArrayList<>();

        Set<Long> addOnServiceIds = bookingReq.getServiceIds() == null
                ? Collections.emptySet()
                : new HashSet<>(bookingReq.getServiceIds());

        /*
         * PACKAGE FLOW
         */
        if (bookingReq.getPackageId() != null) {

            Package pkg = packageRepository
                    .findByPackageIdAndSalonIdAndIsActiveTrue(
                            bookingReq.getPackageId(),
                            bookingReq.getSalonId()
                    )
                    .orElseThrow(() ->
                            new BadRequestException("Selected package is not available for this salon"));

            List<PackageService> packageMappings =
                    packageServiceRepository.findByPackageId(pkg.getPackageId());

            if (packageMappings.isEmpty()) {
                throw new BadRequestException("Selected package has no services configured");
            }

            List<Long> packageServiceIds = packageMappings.stream()
                    .map(PackageService::getServiceId)
                    .toList();

            List<SalonService> packageServices = serviceRepo.findAllById(packageServiceIds);

            if (packageServices.size() != packageServiceIds.size()) {
                throw new BadRequestException("Package contains invalid services");
            }

            grossAmount = grossAmount.add(pkg.getPackagePrice());

            for (SalonService service : packageServices) {
                totalDuration += service.getDurationMinutes();
                bookingServices.add(
                        buildBookingService(service, BigDecimal.ZERO, BookingSourceType.PACKAGE));
            }
        }

        /*
         * ADD-ON FLOW
         */
        if (!addOnServiceIds.isEmpty()) {

            List<SalonService> addOnServices = serviceRepo.findAllById(addOnServiceIds);

            if (addOnServices.size() != addOnServiceIds.size()) {
                throw new BadRequestException("One or more selected services are invalid");
            }

            boolean invalidService = addOnServices.stream()
                    .anyMatch(service ->
                            !service.getSalonId().equals(bookingReq.getSalonId())
                                    || !Boolean.TRUE.equals(service.getIsActive()));

            if (invalidService) {
                throw new BadRequestException("Some selected services are unavailable for this salon");
            }

            for (SalonService service : addOnServices) {
                grossAmount = grossAmount.add(service.getPrice());
                totalDuration += service.getDurationMinutes();
                bookingServices.add(
                        buildBookingService(service, service.getPrice(), BookingSourceType.ADD_ON));
            }
        }

        LocalDateTime startTime = bookingReq.getStartTime();
        LocalDateTime endTime = startTime.plusMinutes(totalDuration);

        //salonResourceRepo.lockSalonResource(bookingReq.getSalonId());
        if (!checkSlotAvailable(bookingReq.getSalonId(), startTime, endTime)) {
            throw new BadRequestException("Selected slot is no longer available");
        }

        PricingResult pricing = calculatePricing(grossAmount);

        Booking booking = Booking.builder()
                .salonId(bookingReq.getSalonId())
                .customerId(bookingReq.getCustomerId())
                .packageId(bookingReq.getPackageId())
                .startTime(startTime)
                .endTime(endTime)
                .grossAmount(pricing.grossAmount())
                .platformFee(pricing.platformFee())
                .taxAmount(pricing.taxAmount())
                .discountAmount(pricing.discountAmount())
                .finalAmount(pricing.finalAmount())
                .partnerAmount(pricing.partnerAmount())
                .status(bookingReq.getStatus() != null ? bookingReq.getStatus() : BookingStatus.PAYMENT_PENDING.name())
                .rejectionReason(bookingReq.getRejectionReason() != null ? bookingReq.getRejectionReason() : null)
                .createdDate(LocalDateTime.now())
                .updatedDate(LocalDateTime.now())
                .build();

        Booking savedBooking = bookingRepo.save(booking);

        bookingServices.forEach(service -> service.setBookingId(savedBooking.getBookingId()));
        bsRepo.saveAll(bookingServices);

        // no payment record created here — done in selectPaymentMode()

        //return buildResponse(savedBooking);
        BookingResponseDTO bookingResponse = buildResponse(savedBooking);

        // Notify customer of new booking creation
        partnerWebSocketService.notifyCustomer(
                booking.getCustomerId(),
                bookingResponse
        );

        return bookingResponse;

    }

    /**
     * Cancels a booking and updates related entities.
     * - Validates booking status (cannot cancel rejected or completed bookings).
     * - Updates booking and associated services to CANCELLED.
     * - Cancels pending payment if applicable.
     *
     * @param bookingId the booking identifier
     */
    @Transactional
    public void cancelBooking(UUID bookingId) {
        // Load booking
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new BadRequestException("Booking not found"));

        // Validate that the logged-in customer is authorized to access this customer record
        authService.validateCustomerAccess(booking.getCustomerId());

        // Validate booking status
        if (BookingStatus.CANCELLED.name().equals(booking.getStatus())) {
            throw new BadRequestException("Booking already cancelled");
        }
        if (BookingStatus.REJECTED.name().equals(booking.getStatus())) {
            throw new BadRequestException("Rejected booking cannot be cancelled");
        }
        if (BookingStatus.COMPLETED.name().equals(booking.getStatus())) {
            throw new BadRequestException("Completed booking cannot be cancelled");
        }
        if (BookingStatus.IN_PROGRESS.name().equals(booking.getStatus())) {
            throw new BadRequestException("In Progress booking cannot be cancelled");
        }

        // Update booking status
        booking.setStatus(BookingStatus.CANCELLED.name());
        booking.setUpdatedDate(LocalDateTime.now());
        bookingRepo.save(booking);

        // Notify customer of new booking creation
        BookingResponseDTO bookingResponse = buildResponse(booking);
        partnerWebSocketService.notifyCustomer(
                booking.getCustomerId(),
                bookingResponse
        );

        // Cancel associated services
        List<BookingServiceEntity> services = bsRepo.findByBookingId(bookingId);
        for (BookingServiceEntity service : services) {
            service.setStatus(BookingServiceStatus.CANCELLED.name());
        }
        bsRepo.saveAll(services);

        // Cancel pending payment if exists
        Payment payment = paymentRepo.findByBookingId(bookingId).orElse(null);
        if (payment != null && PaymentStatus.PENDING.name().equals(payment.getStatus())) {
            payment.setStatus(PaymentStatus.CANCELLED.name());
            payment.setUpdatedDate(LocalDateTime.now());
            paymentRepo.save(payment);
        }
    }

    /**
     * Retrieves all bookings for a given customer and builds response DTOs.
     * Enriches booking data with salon details, payment info, and refund preview.
     *
     * @param userId the customer identifier
     * @return list of BookingResponseDTO containing booking and related details
     */
    public List<BookingResponseDTO> getCustomerBookings(Long customerId) {
        // Validate that the logged-in customer is authorized to access this customer record
        authService.validateCustomerAccess(customerId);

        // Fetch bookings for the customer
        List<Booking> bookings = bookingRepo.findByCustomerId(customerId);
        List<BookingResponseDTO> response = new ArrayList<>();

        // Collect salon and booking IDs for batch queries
        List<Long> salonIds = bookings.stream()
                .map(Booking::getSalonId)
                .toList();

        List<UUID> bookingIds = bookings.stream()
                .map(Booking::getBookingId)
                .toList();

        // Map salon details by salonId
        Map<Long, SalonDetails> salonMap = salonRepo.findAllById(salonIds)
                .stream()
                .collect(Collectors.toMap(
                        SalonDetails::getSalonId,
                        s -> s
                ));

        // Map payments by bookingId
        Map<UUID, Payment> paymentMap = paymentRepo.findByBookingIdIn(bookingIds)
                .stream()
                .collect(Collectors.toMap(
                        Payment::getBookingId,
                        p -> p
                ));

        List<BookingServiceEntity> allBookingServices = bsRepo.findByBookingIdIn(bookingIds);

        Map<UUID, List<BookingServiceEntity>> bookingServiceMap =
                allBookingServices.stream()
                        .collect(Collectors.groupingBy(
                                BookingServiceEntity::getBookingId
                        ));


        // Collect package IDs for batch queries
        Set<Long> packageIds = bookings.stream()
                .map(Booking::getPackageId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Map package details by packageId
        Map<Long, Package> packageMap = packageRepository.findAllById(packageIds)
                .stream()
                .collect(Collectors.toMap(
                        Package::getPackageId,
                        Function.identity()
                ));

        // Map package services by packageId
        List<PackageService> allPackageServices = packageServiceRepository.findByPackageIdIn(packageIds);

        // Group package services by packageId
        Map<Long, List<PackageService>> packageServiceMap = allPackageServices.stream()
                .collect(Collectors.groupingBy(
                        PackageService::getPackageId
                ));

        // Collect all unique service IDs from both booking services and package services
        Set<Long> allServiceIds = Stream.concat(
                allBookingServices.stream().map(BookingServiceEntity::getServiceId),
                allPackageServices.stream().map(PackageService::getServiceId)
        ).collect(Collectors.toSet());

        // Map service details by serviceId
        Map<Long, SalonService> serviceMap = serviceRepo.findAllById(allServiceIds)
                .stream()
                .collect(Collectors.toMap(
                        SalonService::getServiceId,
                        Function.identity()
                ));



        // Build response DTOs
        for (Booking booking : bookings) {
            BookingResponseDTO dto = new BookingResponseDTO();

            dto.setBookingId(booking.getBookingId());
            dto.setGrossAmount(booking.getGrossAmount());
            dto.setPlatformFee(booking.getPlatformFee());
            dto.setDiscountAmount(booking.getDiscountAmount());
            dto.setFinalAmount(booking.getFinalAmount());
            dto.setStatus(booking.getStatus());
            dto.setStartTime(booking.getStartTime());
            dto.setEndTime(booking.getEndTime());
            dto.setCreatedDate(booking.getCreatedDate());
            dto.setRejectionReason(booking.getRejectionReason());

            // Add salon details
            SalonDetails salon = salonMap.get(booking.getSalonId());
            dto.setSalonName(salon != null ? salon.getSalonName() : "Salon");

            // Add payment details and refund preview
            Payment payment = paymentMap.get(booking.getBookingId());
            if (payment != null) {
                dto.setPaymentProvider(payment.getProvider());
                dto.setPaymentStatus(payment.getStatus());

                Map<String, String> refund = getRefundPreview(payment);
                dto.setRefundAmount(refund.get("refundAmount"));
                dto.setRefundTier(refund.get("refundTier"));
            }

            // Package
            if (booking.getPackageId() != null) {
                Package pkg = packageMap.get(booking.getPackageId());

                if (pkg != null) {
                    List<ServiceInfoDTO> packageServices = packageServiceMap
                            .getOrDefault(pkg.getPackageId(), Collections.emptyList())
                            .stream()
                            .map(packageService -> {
                                SalonService service = serviceMap.get(packageService.getServiceId());

                                return ServiceInfoDTO.builder()
                                        .serviceId(service.getServiceId())
                                        .serviceName(service.getServiceName())
                                        .price(service.getPrice())
                                        .durationMinutes(service.getDurationMinutes())
                                        .build();
                            })
                            .toList();

                    dto.setPackageDetails(
                            PackageResponseDTO.builder()
                                    .packageId(pkg.getPackageId())
                                    .salonId(pkg.getSalonId())
                                    .packageName(pkg.getPackageName())
                                    .description(pkg.getDescription())
                                    .packagePrice(pkg.getPackagePrice())
                                    .isActive(pkg.getIsActive())
                                    .services(packageServices)
                                    .build()
                    );
                }
            }

            // Add-On Services
            List<ServiceInfoDTO> addOnServices = bookingServiceMap
                    .getOrDefault(booking.getBookingId(), Collections.emptyList())
                    .stream()
                    .filter(bs -> BookingSourceType.ADD_ON.name().equals(bs.getSourceType()))
                    .map(bs -> {
                        SalonService service = serviceMap.get(bs.getServiceId());

                        return ServiceInfoDTO.builder()
                                .serviceId(bs.getServiceId())
                                .serviceName(service != null ? service.getServiceName() : "Service")
                                .price(bs.getServicePrice())
                                .durationMinutes(bs.getServiceDuration())
                                .build();
                    })
                    .toList();

            dto.setAddOnServices(addOnServices);

            response.add(dto);
        }

        return response;
    }

    /**
     * This method calculates the availability of time slots for a given salon, date, and list of services.
     * It checks the salon's working hours, existing bookings, and resource constraints to determine the status of each slot.
     *
     * @param salonId    The ID of the salon for which to check slot availability.
     * @param serviceIds A list of service IDs that the customer wants to book. The total duration of these services will be used to calculate slot availability.
     * @param date       The date for which to check slot availability.
     * @return A list of SlotResponseDTO objects, each representing a time slot and its availability status (e.g., AVAILABLE, BOOKED, HOLD, UNAVAILABLE, PAST).
     * @throws BadRequestException If the salon or services are not found, or if there are issues with the input parameters.
     */
    public List<SlotResponseDTO> getAvailableSlots(
            Long salonId,
            List<Long> serviceIds,
            LocalDate date
    ) {
        if (date.isBefore(LocalDate.now())) {
            throw new BadRequestException(
                    "The selected date has already passed. Please choose today or a future date."
            );
        }

        // Load Services
        List<SalonService> services = serviceRepo.findAllById(serviceIds);
        int totalDuration = getTotalDuration(serviceIds, services);

        // Load Salon
        SalonDetails salon = salonRepo.findById(salonId)
                .orElseThrow(() -> new BadRequestException("Salon not found"));

        // Load Resources
        SalonResource resource = salonResourceRepo.findBySalonId(salonId)
                .orElseThrow(() -> new BadRequestException("Salon resource config not found"));

        int totalResources = resource.getResourceCount();

        // Active statuses
        List<String> activeStatuses = List.of(
                BookingStatus.PENDING_PARTNER_CONFIRMATION.name(),
                BookingStatus.CONFIRMED.name(),
                BookingStatus.IN_PROGRESS.name()
        );

        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
        LocalDateTime holdThreshold = LocalDateTime.now().minusMinutes(paymentHoldMinutes);

        // Fetch bookings
        List<Booking> bookings = bookingRepo.findBookingsForDate(
                salonId, dayStart, dayEnd, activeStatuses
        );

        // Active payment holds
        Set<UUID> activeHoldBookingIds = new HashSet<>(
                paymentRepo.findActiveHoldBookingIds(holdThreshold)
        );

        LocalDateTime slot = salon.getWorkingHoursStart().atDate(date);
        LocalDateTime closing = salon.getWorkingHoursEnd().atDate(date);
        LocalDateTime now = LocalDateTime.now();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        List<SlotResponseDTO> slots = new ArrayList<>();

        while (slot.isBefore(closing)) {

            LocalDateTime slotStart = slot;
            LocalDateTime slotEnd = slot.plusMinutes(totalDuration);

            SlotStatus status;

            // PAST
            if (date.equals(LocalDate.now()) && slotStart.isBefore(now)) {
                status = SlotStatus.PAST;
            } else {

                // Count occupied resources at slot start
                long occupied = bookings.stream()
                        .filter(b ->
                                activeStatuses.contains(b.getStatus())
                                        || (
                                        BookingStatus.PAYMENT_PENDING.name().equals(b.getStatus())
                                                && activeHoldBookingIds.contains(b.getBookingId())
                                ))
                        .filter(b ->
                                !b.getStartTime().isAfter(slotStart)
                                        && b.getEndTime().isAfter(slotStart))
                        .count();

                // BOOKED
                if (occupied >= totalResources) {
                    status = SlotStatus.BOOKED;
                } else {
                    boolean durationFits = !slotEnd.isAfter(closing);

                    // CASE 1 : Service exceeds salon closing

                    // CASE 2 : Service exceeds next fully occupied period
                    if (durationFits) {
                        for (Booking booking : bookings) {

                            boolean activeBooking =
                                    activeStatuses.contains(booking.getStatus())
                                            || (
                                            BookingStatus.PAYMENT_PENDING.name().equals(booking.getStatus())
                                                    && activeHoldBookingIds.contains(booking.getBookingId())
                                    );

                            if (!activeBooking) {
                                continue;
                            }

                            long resourcesOccupiedAtBookingStart = bookings.stream()
                                    .filter(b ->
                                            activeStatuses.contains(b.getStatus())
                                                    || (
                                                    BookingStatus.PAYMENT_PENDING.name().equals(b.getStatus())
                                                            && activeHoldBookingIds.contains(b.getBookingId())
                                            ))
                                    .filter(b ->
                                            !b.getStartTime().isAfter(booking.getStartTime())
                                                    && b.getEndTime().isAfter(booking.getStartTime()))
                                    .count();

                            // only consider times where all resources become occupied
                            if (resourcesOccupiedAtBookingStart >= totalResources) {
                                if (booking.getStartTime().isAfter(slotStart)
                                        && booking.getStartTime().isBefore(slotEnd)) {
                                    durationFits = false;
                                    break;
                                }
                            }
                        }
                    }

                    if (!durationFits) {
                        //status = SlotStatus.UNAVAILABLE;
                        status = SlotStatus.AVAILABLE;
                    } else {
                        status = SlotStatus.AVAILABLE;
                    }
                }
            }

            slots.add(
                    new SlotResponseDTO(
                            slotStart.toLocalTime().format(formatter),
                            status
                    )
            );

            slot = slot.plusMinutes(slotIntervalMinutes);
        }
        return slots;
    }

    /**
     * Validates the booking request payload.
     * Ensures mandatory fields like salonId, startTime, services/packages,
     * and payment mode are present before processing.
     * Throws BadRequestException if validation fails.
     */
    private void validateRequest(BookingRequestDTO req) {
        // Validate salon ID
        if (req.getSalonId() == null) {
            throw new BadRequestException("Salon is required");
        }

        // Validate booking start time
        if (req.getStartTime() == null) {
            throw new BadRequestException("Booking time is required");
        }

        // Validate service or package selection
        if (req.getPackageId() == null &&
                (req.getServiceIds() == null || req.getServiceIds().isEmpty())) {
            throw new BadRequestException("Please select at least one service or package");
        }
    }

    /**
     * Builds a BookingServiceEntity from the given SalonService, price, and source type.
     * Populates all required fields with initial PENDING status.
     */
    private BookingServiceEntity buildBookingService(
            SalonService service,
            BigDecimal price,
            BookingSourceType sourceType
    ) {
        return BookingServiceEntity.builder()
                .serviceId(service.getServiceId())
                .serviceName(service.getServiceName())
                .servicePrice(price)
                .serviceDuration(service.getDurationMinutes())
                .sourceType(sourceType.name())
                .status(BookingServiceStatus.PENDING.name())
                .build();
    }

    /**
     * Checks if a requested booking slot is available for a given salon.
     * Validates against salon working hours, resource capacity, and overlapping bookings.
     *
     * @param salonId the salon identifier
     * @param start   requested booking start time
     * @param end     requested booking end time
     * @return true if slot is available, false otherwise
     */
    public boolean checkSlotAvailable(Long salonId, LocalDateTime start, LocalDateTime end) {
        if (!start.isAfter(LocalDateTime.now())) {
            throw new BadRequestException("Cannot book a past slot");
        }

        // Load salon details
        SalonDetails salon = salonRepo.findById(salonId)
                .orElseThrow(() -> new BadRequestException("Salon not found"));

        // Validate requested time against salon working hours
        LocalDateTime salonOpen = salon.getWorkingHoursStart().atDate(start.toLocalDate());
        LocalDateTime salonClose = salon.getWorkingHoursEnd().atDate(start.toLocalDate());
        if (start.isBefore(salonOpen)) { //  || end.isAfter(salonClose)
            return false;
        }

        // Active booking statuses considered for overlap
        List<String> activeStatuses = List.of(
                BookingStatus.PENDING_PARTNER_CONFIRMATION.name(),
                BookingStatus.CONFIRMED.name(),
                BookingStatus.IN_PROGRESS.name()
        );

        // Load salon resource configuration
        SalonResource resource = salonResourceRepo.findBySalonId(salonId)
                .orElseThrow(() -> new BadRequestException("Salon resource config not found"));

        // Get overlapping bookings
        List<Booking> overlappingBookings = bookingRepo.findOverlappingBookings(
                salonId,
                start,
                end,
                activeStatuses
        );

// Active HOLD bookings (Payment INITIATED within hold window)
        Set<UUID> activeHoldBookingIds = new HashSet<>(
                paymentRepo.findActiveHoldBookingIds(
                        LocalDateTime.now().minusMinutes(paymentHoldMinutes)
                )
        );

        long occupied = overlappingBookings.stream()
                .filter(booking ->
                        activeStatuses.contains(booking.getStatus())
                                || (
                                BookingStatus.PAYMENT_PENDING.name().equals(booking.getStatus())
                                        && activeHoldBookingIds.contains(booking.getBookingId())
                        )
                )
                .count();

        //return !bookingRepo.existsOverlappingBooking(salonId, start, end, activeStatuses);
        return occupied < resource.getResourceCount();
    }

    /**
     * Calculates pricing breakdown for a booking.
     * Applies platform fee if gross > 0, sets discount to zero,
     * and computes final and partner amounts.
     *
     * @param gross the gross service amount
     * @return PricingResult containing gross, platform fee, discount, final amount, and partner amount
     */
    private PricingResult calculatePricing(BigDecimal grossAmount) {

        if (grossAmount == null || grossAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return new PricingResult(
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO
            );
        }

        // Fixed platform fee charged to customer
        BigDecimal platformFee = BigDecimal.valueOf(15);

        BigDecimal commissionPercentage = BigDecimal.ZERO;

        // 0% commission retained by platform
        BigDecimal commissionAmount = grossAmount
                .multiply(commissionPercentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        // Future coupon discounts
        BigDecimal discountAmount = BigDecimal.ZERO;

        // GST on platform fee (18%)
        BigDecimal taxAmount = platformFee
                .multiply(BigDecimal.valueOf(0.18))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal finalAmount = grossAmount
                .add(platformFee)
                .add(taxAmount)
                .subtract(discountAmount);

        BigDecimal partnerAmount = grossAmount
                .subtract(commissionAmount);

        return new PricingResult(
                grossAmount,
                platformFee,
                commissionAmount,
                discountAmount,
                taxAmount,
                finalAmount,
                partnerAmount
        );
    }

    /**
     * Provides a price summary for a booking request without creating a booking.
     * Calculates gross amount based on selected package and add-on services,
     * then applies pricing rules to compute platform fee, discount, and final amount.
     *
     * @param request the price summary request DTO containing salonId, packageId, and serviceIds
     * @return BookingResponseDTO containing the calculated price breakdown
     */
    public BookingResponseDTO getPriceSummary(PriceSummaryRequestDTO request) {

        // Validate service or package selection
        if (request.getPackageId() == null &&
                (request.getServiceIds() == null || request.getServiceIds().isEmpty())) {
            throw new BadRequestException("Please select at least one service or package");
        }

        BigDecimal grossAmount = BigDecimal.ZERO;

        // Package amount
        if (request.getPackageId() != null) {
            Package pkg = packageRepository
                    .findByPackageIdAndSalonIdAndIsActiveTrue(
                            request.getPackageId(),
                            request.getSalonId()
                    )
                    .orElseThrow(() -> new BadRequestException(
                            "Selected package is not available for this salon"
                    ));

            grossAmount = grossAmount.add(pkg.getPackagePrice());
        }

        // Add-on services amount
        if (request.getServiceIds() != null && !request.getServiceIds().isEmpty()) {
            List<SalonService> addOnServices = serviceRepo.findAllById(request.getServiceIds());

            for (SalonService service : addOnServices) {
                grossAmount = grossAmount.add(service.getPrice());
            }
        }
        PricingResult pricing = calculatePricing(grossAmount);

        return new BookingResponseDTO(
                pricing.grossAmount(),
                pricing.platformFee(),
                pricing.commissionAmount(),
                pricing.discountAmount(),
                pricing.taxAmount(),
                pricing.finalAmount()
        );
    }


    /**
     * Builds a BookingResponseDTO from a Booking entity.
     * Calculates total duration in minutes and maps all relevant fields.
     *
     * @param booking the booking entity
     * @return BookingResponseDTO containing booking details and computed duration
     */
    public BookingResponseDTO buildResponse(Booking booking) {
        // Calculate total duration in minutes
        Long totalDuration = Duration.between(
                booking.getStartTime(),
                booking.getEndTime()
        ).toMinutes();

        // Build response DTO
        return new BookingResponseDTO(
                booking.getBookingId(),
                booking.getGrossAmount(),
                booking.getPlatformFee(),
                booking.getDiscountAmount(),
                booking.getFinalAmount(),
                booking.getStatus(),
                booking.getStartTime(),
                booking.getEndTime(),
                totalDuration
        );
    }

    /**
     * Calculates the refund amount based on the time elapsed since payment creation.
     * - Full refund if within 10 minutes
     * - 50% refund if within 60 minutes
     * - No refund after 60 minutes
     *
     * @param payment the payment entity
     * @return refund amount as BigDecimal
     */
    private BigDecimal calculateRefundAmount(Payment payment) {
        // Validate payment creation timestamp
        if (payment.getCreatedDate() == null) {
            return BigDecimal.ZERO;
        }

        // Calculate elapsed time in minutes
        long minutes = Duration.between(
                payment.getCreatedDate(),
                LocalDateTime.now()
        ).toMinutes();

        // Full refund if within 10 minutes
        if (minutes <= 10) {
            return payment.getAmount();
        }

        // 50% refund if within 60 minutes
        if (minutes <= 60) {
            return payment.getAmount()
                    .multiply(new BigDecimal("0.5"))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // No refund after 60 minutes
        return BigDecimal.ZERO;
    }

    /**
     * Generates a preview of the refund outcome for a given payment.
     * Determines refund amount and categorizes it into FULL, PARTIAL, or NONE.
     *
     * @param payment the payment entity
     * @return map containing refundAmount and refundTier
     */
    public Map<String, String> getRefundPreview(Payment payment) {
        // Calculate refund amount
        BigDecimal refund = calculateRefundAmount(payment);

        // Determine refund tier
        String tier = refund.compareTo(payment.getAmount()) == 0
                ? "FULL"
                : refund.compareTo(BigDecimal.ZERO) > 0
                ? "PARTIAL"
                : "NONE";

        // Build response map
        Map<String, String> map = new HashMap<>();
        map.put("refundAmount", refund.toPlainString());
        map.put("refundTier", tier);

        return map;
    }

   /**
     * Calculates the total duration in minutes for a list of service IDs.
     * Handles both package and add-on services, accounting for duplicates in service selection.
     *
     * @param serviceIds list of service IDs selected by the customer
     * @param services   list of SalonService entities corresponding to the service IDs
     * @return total duration in minutes for the selected services
     * @throws BadRequestException if the services list is empty or if there are invalid service IDs
     */
    private static int getTotalDuration(List<Long> serviceIds, List<SalonService> services) {
        if (services.isEmpty()) {
            throw new BadRequestException("Services not found");
        }

        // Calculate Total Duration (respect duplicates in serviceIds)
        Map<Long, Integer> durationMap = new HashMap<>(services.size());
        for (SalonService s : services) {
            durationMap.put(s.getServiceId(), s.getDurationMinutes());
        }
        int totalDuration = 0;
        for (Long id : serviceIds) {
            Integer d = durationMap.get(id);
            if (d != null) {
                totalDuration += d; // add duration for each occurrence of serviceId
            }
        }
        return totalDuration;
    }

    /**
     * Immutable pricing result for a booking.
     * Contains gross amount, platform fee, discount, final amount, and partner amount.
     */
    public record PricingResult(
            BigDecimal grossAmount,
            BigDecimal platformFee,
            BigDecimal commissionAmount,
            BigDecimal discountAmount,
            BigDecimal taxAmount,
            BigDecimal finalAmount,
            BigDecimal partnerAmount
    ) {
    }
}