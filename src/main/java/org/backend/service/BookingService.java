package org.backend.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.backend.dto.booking.BookingRequestDTO;
import org.backend.dto.booking.BookingResponseDTO;
import org.backend.enums.BookingServiceStatus;
import org.backend.enums.BookingSourceType;
import org.backend.enums.BookingStatus;
import org.backend.enums.PaymentMode;
import org.backend.enums.PaymentStatus;
import org.backend.exception.BadRequestException;
import org.backend.model.*;
import org.backend.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.backend.model.Package;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepo;
    private final BookingServiceRepository bsRepo;
    private final SalonServiceRepository serviceRepo;
    private final PaymentRepository paymentRepo;
    private final SalonRepository salonRepo;
    private final PackageServiceRepository packageServiceRepository;
    private final PackageRepository packageRepository;
    private final SalonResourceRepository salonResourceRepo;

    @Value("${app.booking.payment-hold-minutes}")
    private int paymentHoldMinutes;

    /*
     * CREATE BOOKING
     */
    @Transactional
    public BookingResponseDTO createBooking(BookingRequestDTO bookingReq) {

        validateRequest(bookingReq);

        PaymentMode paymentMode = PaymentMode.valueOf(bookingReq.getPaymentMode().toUpperCase());

        // idempotency check - prevent duplicate pending bookings for same slot
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
                    .findByPackageIdAndSalonIdAndIsActiveTrue(bookingReq.getPackageId(), bookingReq.getSalonId())
                    .orElseThrow(() ->
                            new BadRequestException(
                                    "Selected package is not available for this salon"
                            ));

            List<PackageService> packageMappings = packageServiceRepository.findByPackageId(pkg.getPackageId());

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

            List<String> duplicateServices = packageServices.stream()
                    .filter(service ->
                            addOnServiceIds.contains(service.getServiceId())
                    )
                    .map(SalonService::getServiceName)
                    .toList();

            if (!duplicateServices.isEmpty()) {
                throw new BadRequestException(
                        "These services are already included in selected package: "
                                + String.join(", ", duplicateServices)
                );
            }

            grossAmount = grossAmount.add(pkg.getPackagePrice());

            for (SalonService service : packageServices) {
                totalDuration += service.getDurationMinutes();
                bookingServices.add(buildBookingService(service, BigDecimal.ZERO, BookingSourceType.PACKAGE));
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
                                    || !Boolean.TRUE.equals(service.getIsActive())
                    );

            if (invalidService) {
                throw new BadRequestException("Some selected services are unavailable for this salon");
            }

            for (SalonService service : addOnServices) {

                grossAmount = grossAmount.add(service.getPrice());
                totalDuration += service.getDurationMinutes();

                bookingServices.add(
                        buildBookingService(
                                service,
                                service.getPrice(),
                                BookingSourceType.ADD_ON
                        )
                );
            }
        }

        LocalDateTime startTime = bookingReq.getStartTime();
        LocalDateTime endTime = startTime.plusMinutes(totalDuration);

        if (!checkSlotAvailable(bookingReq.getSalonId(), startTime, endTime)) {
            throw new BadRequestException("Selected slot is no longer available");
        }

        PricingResult pricing = calculatePricing(grossAmount);

        String bookingStatus = paymentMode == PaymentMode.ONLINE
                ? BookingStatus.PAYMENT_PENDING.name()
                : BookingStatus.PENDING_PARTNER_CONFIRMATION.name();

        Booking booking = Booking.builder()
                .salonId(bookingReq.getSalonId())
                .customerId(bookingReq.getCustomerId())
                .packageId(bookingReq.getPackageId())
                .startTime(startTime)
                .endTime(endTime)
                .grossAmount(pricing.grossAmount())
                .platformFee(pricing.platformFee())
                .taxAmount(BigDecimal.ZERO)
                .discountAmount(pricing.discount())
                .finalAmount(pricing.finalAmount())
                .partnerAmount(pricing.partnerAmount())
                .status(bookingStatus)
                .createdDate(LocalDateTime.now())
                .updatedDate(LocalDateTime.now())
                .build();

        Booking savedBooking = bookingRepo.save(booking);

        bookingServices.forEach(service ->
                service.setBookingId(savedBooking.getBookingId()));

        bsRepo.saveAll(bookingServices);

        /*
         * PAYMENT RECORD
         */
        Payment payment = Payment.builder()
                .bookingId(savedBooking.getBookingId())
                .amount(pricing.finalAmount())
                .currency("INR")
                .status(PaymentStatus.PENDING.name())
                .provider(paymentMode == PaymentMode.ONLINE ? "RAZORPAY" : "PAY_AT_SALON")
                .build();

        paymentRepo.save(payment);

        return buildResponse(savedBooking);
    }

    /*
     * CANCEL BOOKING
     */
    @Transactional
    public void cancelBooking(Long bookingId) {

        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new BadRequestException("Booking not found"));

        if (BookingStatus.CANCELLED.name().equals(booking.getStatus())) {
            throw new BadRequestException("Booking already cancelled");
        }

        if (BookingStatus.REJECTED.name().equals(booking.getStatus())) {
            throw new BadRequestException("Rejected booking cannot be cancelled");
        }

        if (BookingStatus.COMPLETED.name().equals(booking.getStatus())) {
            throw new BadRequestException("Completed booking cannot be cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED.name());
        booking.setUpdatedDate(LocalDateTime.now());

        bookingRepo.save(booking);

        List<BookingServiceEntity> services = bsRepo.findByBookingId(bookingId);

        for (BookingServiceEntity service : services) {
            service.setStatus(BookingServiceStatus.CANCELLED.name());
        }

        bsRepo.saveAll(services);

        Payment payment = paymentRepo.findByBookingId(bookingId).orElse(null);

        if (payment != null && PaymentStatus.PENDING.name().equals(payment.getStatus())) {

            payment.setStatus(PaymentStatus.CANCELLED.name());
            payment.setUpdatedDate(LocalDateTime.now());

            paymentRepo.save(payment);
        }
    }

    /*
     * GET CUSTOMER BOOKINGS
     */
    public List<BookingResponseDTO> getCustomerBookings(Long userId) {

        List<Booking> bookings = bookingRepo.findByCustomerId(userId);
        List<BookingResponseDTO> response = new ArrayList<>();

        List<Long> salonIds = bookings.stream()
                .map(Booking::getSalonId)
                .toList();

        List<Long> bookingIds = bookings.stream()
                .map(Booking::getBookingId)
                .toList();

        Map<Long, SalonDetails> salonMap =
                salonRepo.findAllById(salonIds)
                        .stream()
                        .collect(Collectors.toMap(
                                SalonDetails::getSalonId,
                                s -> s
                        ));

        Map<Long, Payment> paymentMap =
                paymentRepo.findByBookingIdIn(bookingIds)
                        .stream()
                        .collect(Collectors.toMap(
                                Payment::getBookingId,
                                p -> p
                        ));

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

            SalonDetails salon = salonMap.get(booking.getSalonId());

            dto.setSalonName(salon != null ? salon.getSalonName() : "Salon");

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

    /*
     * AVAILABLE SLOTS
     */
    public List<String> getAvailableSlots(Long salonId, List<Long> serviceIds, LocalDate date) {


        List<SalonService> services = serviceRepo.findAllById(serviceIds);

        if (services.isEmpty()) {
            throw new BadRequestException("Services not found");
        }

        int totalDuration = services.stream()
                .mapToInt(SalonService::getDurationMinutes)
                .sum();

        SalonDetails salon = salonRepo.findById(salonId)
                .orElseThrow(() -> new BadRequestException("Salon not found"));

        SalonResource resource = salonResourceRepo.findBySalonId(salonId)
                .orElseThrow(() -> new BadRequestException("Salon resource config not found"));

        int totalResources = resource.getResourceCount();

        List<String> activeStatuses = List.of(
                BookingStatus.PENDING_PARTNER_CONFIRMATION.name(),
                BookingStatus.CONFIRMED.name(),
                BookingStatus.IN_PROGRESS.name()
        );

        List<Booking> bookings = bookingRepo.findBookingsForDate(
                salonId,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay(),
                activeStatuses,
                LocalDateTime.now().minusMinutes(paymentHoldMinutes)
        );

        List<String> availableSlots = new ArrayList<>();

        LocalDateTime slot = salon.getWorkingHoursStart().atDate(date);
        LocalDateTime closing = salon.getWorkingHoursEnd().atDate(date);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");

        while (!slot.plusMinutes(totalDuration).isAfter(closing)) {

            if (date.equals(LocalDate.now()) && slot.isBefore(LocalDateTime.now())) {
                slot = slot.plusMinutes(15);
                continue;
            }

            LocalDateTime currentSlot = slot;
            LocalDateTime end = currentSlot.plusMinutes(totalDuration);

            // single resource scenario
//            boolean overlap = bookings.stream().anyMatch(
//                    booking ->
//                            booking.getStartTime().isBefore(end)
//                                    && booking.getEndTime().isAfter(currentSlot)
//            );
//
//            if (!overlap) {
//                availableSlots.add(currentSlot.toLocalTime().format(formatter));
//            }
            // single resource scenario end


            // multi resource scenario - to be implemented when we have multiple resources per salon
             long overlappingCount = bookings.stream()
                     .filter(
                    booking ->
                            booking.getStartTime().isBefore(end) && booking.getEndTime().isAfter(currentSlot)
                     ).count();

            if (overlappingCount < totalResources) {
                availableSlots.add(currentSlot.toLocalTime().format(formatter));
            }
            // multi resource scenario end

            slot = slot.plusMinutes(15);
        }

        return availableSlots;
    }

    /*
     * HELPERS
     */
    private void validateRequest(BookingRequestDTO req) {

        if (req.getSalonId() == null) {
            throw new BadRequestException("Salon is required");
        }

        if (req.getStartTime() == null) {
            throw new BadRequestException("Booking time is required");
        }

        if (req.getPackageId() == null &&
                (req.getServiceIds() == null || req.getServiceIds().isEmpty())) {
            throw new BadRequestException(
                    "Please select at least one service or package"
            );
        }

        if (req.getPaymentMode() == null ||
                req.getPaymentMode().isBlank()) {
            throw new BadRequestException(
                    "Payment mode is required"
            );
        }
    }

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

    private boolean checkSlotAvailable(Long salonId, LocalDateTime start, LocalDateTime end) {
        SalonDetails salon = salonRepo.findById(salonId)
                .orElseThrow(() -> new BadRequestException("Salon not found"));

        // timing validation
        LocalDateTime salonOpen = salon.getWorkingHoursStart().atDate(start.toLocalDate());
        LocalDateTime salonClose = salon.getWorkingHoursEnd().atDate(start.toLocalDate());

        if (start.isBefore(salonOpen) || end.isAfter(salonClose)) {
            return false;
        }

        List<String> activeStatuses = List.of(
                BookingStatus.PENDING_PARTNER_CONFIRMATION.name(),
                BookingStatus.CONFIRMED.name(),
                BookingStatus.IN_PROGRESS.name()
        );

        SalonResource resource = salonResourceRepo.findBySalonId(salonId)
                .orElseThrow(() -> new BadRequestException("Salon resource config not found"));

        long overlappingCount = bookingRepo.countOverlappingBookings(
                salonId, start, end, activeStatuses, LocalDateTime.now().minusMinutes(paymentHoldMinutes));

        //return !bookingRepo.existsOverlappingBooking(salonId, start, end, activeStatuses);
        return overlappingCount < resource.getResourceCount();

    }

    private PricingResult calculatePricing(BigDecimal gross) {

        BigDecimal platformFee =
                gross.compareTo(BigDecimal.ZERO) > 0
                        ? BigDecimal.valueOf(15)
                        : BigDecimal.ZERO;

        BigDecimal discount = BigDecimal.ZERO;

        BigDecimal finalAmount = gross.add(platformFee);

        BigDecimal partnerAmount = gross;

        return new PricingResult(
                gross,
                platformFee,
                discount,
                finalAmount,
                partnerAmount
        );
    }

    private BookingResponseDTO buildResponse(Booking booking) {

        Long totalDuration = Duration.between(
                booking.getStartTime(),
                booking.getEndTime()
        ).toMinutes();

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

    private BigDecimal calculateRefundAmount(Payment payment) {

        if (payment.getCreatedDate() == null) {
            return BigDecimal.ZERO;
        }

        long minutes = Duration.between(
                payment.getCreatedDate(),
                LocalDateTime.now()
        ).toMinutes();

        if (minutes <= 10) {
            return payment.getAmount();
        }

        if (minutes <= 60) {
            return payment.getAmount()
                    .multiply(new BigDecimal("0.5"))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return BigDecimal.ZERO;
    }

    public Map<String, String> getRefundPreview(Payment payment) {

        BigDecimal refund = calculateRefundAmount(payment);

        String tier =
                refund.compareTo(payment.getAmount()) == 0
                        ? "FULL"
                        : refund.compareTo(BigDecimal.ZERO) > 0
                        ? "PARTIAL"
                        : "NONE";

        Map<String, String> map = new HashMap<>();
        map.put("refundAmount", refund.toPlainString());
        map.put("refundTier", tier);

        return map;
    }

    public record PricingResult(
            BigDecimal grossAmount,
            BigDecimal platformFee,
            BigDecimal discount,
            BigDecimal finalAmount,
            BigDecimal partnerAmount
    ) {
    }
}