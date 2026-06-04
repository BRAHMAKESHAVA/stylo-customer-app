package org.backend.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

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

//            List<String> duplicateServices = packageServices.stream()
//                    .filter(service -> addOnServiceIds.contains(service.getServiceId()))
//                    .map(SalonService::getServiceName)
//                    .toList();
//
//            if (!duplicateServices.isEmpty()) {
//                throw new BadRequestException(
//                        STR."These services are already included in selected package: \{String.join(", ", duplicateServices)}");
//            }

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
                .status(BookingStatus.PAYMENT_PENDING.name())
                .createdDate(LocalDateTime.now())
                .updatedDate(LocalDateTime.now())
                .build();

        Booking savedBooking = bookingRepo.save(booking);

        bookingServices.forEach(service -> service.setBookingId(savedBooking.getBookingId()));
        bsRepo.saveAll(bookingServices);

        // no payment record created here — done in selectPaymentMode()

        return buildResponse(savedBooking);
    }

//    /**
//     * Creates a new booking for a customer.
//     * - Validates request payload and prevents duplicate pending bookings.
//     * - Handles both package and add-on service flows.
//     * - Calculates pricing, duration, and slot availability.
//     * - Persists booking, associated services, and payment record.
//     *
//     * @param bookingReq the booking request DTO
//     * @return BookingResponseDTO containing booking details
//     */
//    @Transactional
//    public BookingResponseDTO createBooking(BookingRequestDTO bookingReq) {
//        // Validate request payload
//        validateRequest(bookingReq);
//
//        PaymentMode paymentMode = PaymentMode.valueOf(bookingReq.getPaymentMode().toUpperCase());
//
//        // Idempotency check: prevent duplicate pending bookings for same slot
//        Optional<Booking> existing = bookingRepo
//                .findFirstByCustomerIdAndSalonIdAndStartTimeAndStatus(
//                        bookingReq.getCustomerId(),
//                        bookingReq.getSalonId(),
//                        bookingReq.getStartTime(),
//                        BookingStatus.PAYMENT_PENDING.name()
//                );
//
//        if (existing.isPresent()) {
//            Booking existingBooking = existing.get();
//            if (existingBooking.getCreatedDate()
//                    .isAfter(LocalDateTime.now().minusMinutes(paymentHoldMinutes))) {
//                return buildResponse(existingBooking);
//            }
//        }
//
//        BigDecimal grossAmount = BigDecimal.ZERO;
//        Long totalDuration = 0L;
//        List<BookingServiceEntity> bookingServices = new ArrayList<>();
//
//        // Collect add-on service IDs
//        Set<Long> addOnServiceIds = bookingReq.getServiceIds() == null
//                ? Collections.emptySet()
//                : new HashSet<>(bookingReq.getServiceIds());
//
//        /*
//         * PACKAGE FLOW
//         */
//        if (bookingReq.getPackageId() != null) {
//            Package pkg = packageRepository
//                    .findByPackageIdAndSalonIdAndIsActiveTrue(bookingReq.getPackageId(), bookingReq.getSalonId())
//                    .orElseThrow(() ->
//                            new BadRequestException("Selected package is not available for this salon"));
//
//            List<PackageService> packageMappings = packageServiceRepository.findByPackageId(pkg.getPackageId());
//            if (packageMappings.isEmpty()) {
//                throw new BadRequestException("Selected package has no services configured");
//            }
//
//            List<Long> packageServiceIds = packageMappings.stream()
//                    .map(PackageService::getServiceId)
//                    .toList();
//
//            List<SalonService> packageServices = serviceRepo.findAllById(packageServiceIds);
//            if (packageServices.size() != packageServiceIds.size()) {
//                throw new BadRequestException("Package contains invalid services");
//            }
//
//            // Prevent duplicate services between package and add-ons
//            List<String> duplicateServices = packageServices.stream()
//                    .filter(service -> addOnServiceIds.contains(service.getServiceId()))
//                    .map(SalonService::getServiceName)
//                    .toList();
//            if (!duplicateServices.isEmpty()) {
//                throw new BadRequestException(
//                        STR."These services are already included in selected package: \{String.join(", ", duplicateServices)}");
//            }
//
//            grossAmount = grossAmount.add(pkg.getPackagePrice());
//            for (SalonService service : packageServices) {
//                totalDuration += service.getDurationMinutes();
//                bookingServices.add(buildBookingService(service, BigDecimal.ZERO, BookingSourceType.PACKAGE));
//            }
//        }
//
//        /*
//         * ADD-ON FLOW
//         */
//        if (!addOnServiceIds.isEmpty()) {
//            List<SalonService> addOnServices = serviceRepo.findAllById(addOnServiceIds);
//            if (addOnServices.size() != addOnServiceIds.size()) {
//                throw new BadRequestException("One or more selected services are invalid");
//            }
//
//            boolean invalidService = addOnServices.stream()
//                    .anyMatch(service ->
//                            !service.getSalonId().equals(bookingReq.getSalonId())
//                                    || !Boolean.TRUE.equals(service.getIsActive()));
//            if (invalidService) {
//                throw new BadRequestException("Some selected services are unavailable for this salon");
//            }
//
//            for (SalonService service : addOnServices) {
//                grossAmount = grossAmount.add(service.getPrice());
//                totalDuration += service.getDurationMinutes();
//                bookingServices.add(buildBookingService(service, service.getPrice(), BookingSourceType.ADD_ON));
//            }
//        }
//
//        // Calculate booking times
//        LocalDateTime startTime = bookingReq.getStartTime();
//        LocalDateTime endTime = startTime.plusMinutes(totalDuration);
//
//        // Validate slot availability
//        if (!checkSlotAvailable(bookingReq.getSalonId(), startTime, endTime)) {
//            throw new BadRequestException("Selected slot is no longer available");
//        }
//
//        // Pricing calculation
//        PricingResult pricing = calculatePricing(grossAmount);
//
//        // Determine booking status based on payment mode
//        String bookingStatus = paymentMode == PaymentMode.ONLINE
//                ? BookingStatus.PAYMENT_PENDING.name()
//                : BookingStatus.PENDING_PARTNER_CONFIRMATION.name();
//
//        // Build booking entity
//        Booking booking = Booking.builder()
//                .salonId(bookingReq.getSalonId())
//                .customerId(bookingReq.getCustomerId())
//                .packageId(bookingReq.getPackageId())
//                .startTime(startTime)
//                .endTime(endTime)
//                .grossAmount(pricing.grossAmount())
//                .platformFee(pricing.platformFee())
//                .taxAmount(BigDecimal.ZERO)
//                .discountAmount(pricing.discount())
//                .finalAmount(pricing.finalAmount())
//                .partnerAmount(pricing.partnerAmount())
//                .status(bookingStatus)
//                .createdDate(LocalDateTime.now())
//                .updatedDate(LocalDateTime.now())
//                .build();
//
//        Booking savedBooking = bookingRepo.save(booking);
//
//        // Link services to booking
//        bookingServices.forEach(service -> service.setBookingId(savedBooking.getBookingId()));
//        bsRepo.saveAll(bookingServices);
//
//        /*
//         * PAYMENT RECORD
//         */
//        Payment payment = Payment.builder()
//                .bookingId(savedBooking.getBookingId())
//                .amount(pricing.finalAmount())
//                .currency("INR")
//                .status(PaymentStatus.PENDING.name())
//                .provider(paymentMode == PaymentMode.ONLINE ? "RAZORPAY" : "PAY_AT_SALON")
//                .build();
//
//        paymentRepo.save(payment);
//
//        return buildResponse(savedBooking);
//    }

    /**
     * Cancels a booking and updates related entities.
     * - Validates booking status (cannot cancel rejected or completed bookings).
     * - Updates booking and associated services to CANCELLED.
     * - Cancels pending payment if applicable.
     *
     * @param bookingId the booking identifier
     */
    @Transactional
    public void cancelBooking(Long bookingId) {
        // Load booking
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new BadRequestException("Booking not found"));

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

        List<Long> bookingIds = bookings.stream()
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
        Map<Long, Payment> paymentMap = paymentRepo.findByBookingIdIn(bookingIds)
                .stream()
                .collect(Collectors.toMap(
                        Payment::getBookingId,
                        p -> p
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
            dto.setCreatedDate(booking.getCreatedDate());
            dto.setRejectionReason(booking.getRejectionReason());

            // Add salon details
            SalonDetails salon = salonMap.get(booking.getSalonId());
            dto.setSalonName(salon != null ? salon.getSalonName() : "Salon");

            // Add payment details and refund preview
            Payment payment = paymentMap.get(booking.getBookingId());
            if (payment != null) {
                dto.setPaymentProvider(payment.getProvider());

                Map<String, String> refund = getRefundPreview(payment);
                dto.setRefundAmount(refund.get("refundAmount"));
                dto.setRefundTier(refund.get("refundTier"));
            }

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
        Set<Long> activeHoldBookingIds = new HashSet<>(
                paymentRepo.findActiveHoldBookingIds(holdThreshold)
        );

        LocalDateTime slot = salon.getWorkingHoursStart().atDate(date);
        LocalDateTime closing = salon.getWorkingHoursEnd().atDate(date);
        LocalDateTime now = LocalDateTime.now();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        List<SlotResponseDTO> slots = new ArrayList<>();

        while (!slot.plusMinutes(totalDuration).isAfter(closing)) {
            LocalDateTime slotStart = slot;
            LocalDateTime slotEnd = slot.plusMinutes(totalDuration);

            SlotStatus status;

            // Past Slot
            if (date.equals(LocalDate.now()) && slotStart.isBefore(now)) {
                status = SlotStatus.PAST;
            } else {
                // BOOKED RESOURCES: Booking occupies the slot start time
                long bookedOverlap = bookings.stream()
                        .filter(b -> !BookingStatus.PAYMENT_PENDING.name().equals(b.getStatus()))
                        .filter(b -> !b.getStartTime().isAfter(slotStart) && b.getEndTime().isAfter(slotStart))
                        .count();

                // HOLD RESOURCES: PAYMENT_PENDING + INITIATED
                long holdOverlap = bookings.stream()
                        .filter(b -> BookingStatus.PAYMENT_PENDING.name().equals(b.getStatus()))
                        .filter(b -> activeHoldBookingIds.contains(b.getBookingId()))
                        .filter(b -> !b.getStartTime().isAfter(slotStart) && b.getEndTime().isAfter(slotStart))
                        .count();

                // SERVICE FIT CHECK: Any booking starts inside service window
                long intrudes = bookings.stream()
                        .filter(b -> !BookingStatus.PAYMENT_PENDING.name().equals(b.getStatus())
                                || activeHoldBookingIds.contains(b.getBookingId()))
                        .filter(b -> b.getStartTime().isAfter(slotStart) && b.getStartTime().isBefore(slotEnd))
                        .count();

                // PRIORITY
                if (bookedOverlap >= totalResources) {
                    status = SlotStatus.BOOKED;
                } else if (holdOverlap >= totalResources) {
                    status = SlotStatus.HOLD;
                } else if (intrudes > 0) {
                    status = SlotStatus.UNAVAILABLE;
                } else {
                    status = SlotStatus.AVAILABLE;
                }
            }

            slots.add(new SlotResponseDTO(
                    slotStart.toLocalTime().format(formatter),
                    status
            ));

            slot = slot.plusMinutes(slotIntervalMinutes);
        }

        return slots;
    }


    /*
     * HELPERS
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
    private boolean checkSlotAvailable(Long salonId, LocalDateTime start, LocalDateTime end) {
        // Load salon details
        SalonDetails salon = salonRepo.findById(salonId)
                .orElseThrow(() -> new BadRequestException("Salon not found"));

        // Validate requested time against salon working hours
        LocalDateTime salonOpen = salon.getWorkingHoursStart().atDate(start.toLocalDate());
        LocalDateTime salonClose = salon.getWorkingHoursEnd().atDate(start.toLocalDate());
        if (start.isBefore(salonOpen) || end.isAfter(salonClose)) {
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

        // Count overlapping bookings within resource capacity
        long overlappingCount = bookingRepo.countOverlappingBookings(
                salonId, start, end, activeStatuses, LocalDateTime.now().minusMinutes(paymentHoldMinutes));

        //return !bookingRepo.existsOverlappingBooking(salonId, start, end, activeStatuses);
        return overlappingCount < resource.getResourceCount();
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
    private BookingResponseDTO buildResponse(Booking booking) {
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
    ) {}
}