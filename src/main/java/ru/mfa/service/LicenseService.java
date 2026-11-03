package ru.mfa.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mfa.dto.Ticket;
import ru.mfa.dto.TicketResponse;
import ru.mfa.model.*;
import ru.mfa.repository.*;
import ru.mfa.signature.DigitalSignatureService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class LicenseService {

    private final LicenseRepository licenseRepository;
    private final ProductRepository productRepository;
    private final LicenseTypeRepository licenseTypeRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceLicenseRepository deviceLicenseRepository;
    private final LicenseHistoryRepository licenseHistoryRepository;
    private final DigitalSignatureService digitalSignatureService;

    public LicenseService(LicenseRepository licenseRepository,
                          ProductRepository productRepository,
                          LicenseTypeRepository licenseTypeRepository,
                          DeviceRepository deviceRepository,
                          DeviceLicenseRepository deviceLicenseRepository,
                          LicenseHistoryRepository licenseHistoryRepository,
                          DigitalSignatureService digitalSignatureService) {
        this.licenseRepository = licenseRepository;
        this.productRepository = productRepository;
        this.licenseTypeRepository = licenseTypeRepository;
        this.deviceRepository = deviceRepository;
        this.deviceLicenseRepository = deviceLicenseRepository;
        this.licenseHistoryRepository = licenseHistoryRepository;
        this.digitalSignatureService = digitalSignatureService;
    }

    @Transactional
    public License createLicense(UUID productId, UUID typeId, UUID ownerId,
                                 int deviceCount, String description) {
        if (deviceCount < 1) {
            throw new IllegalArgumentException("deviceCount must be at least 1");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        if (Boolean.TRUE.equals(product.getIsBlocked())) {
            throw new RuntimeException("Product is blocked");
        }
        LicenseType type = licenseTypeRepository.findById(typeId)
                .orElseThrow(() -> new RuntimeException("License type not found"));

        License license = new License();
        license.setCode(UUID.randomUUID().toString());
        license.setProductId(productId);
        license.setTypeId(typeId);
        license.setOwnerId(ownerId);
        license.setDeviceCount(deviceCount);
        license.setDescription(description);
        license.setBlocked(false);
        license.setFirstActivationDate(null);
        license.setEndingDate(LocalDate.now().plusDays(type.getDefaultDurationInDays()));

        License saved = licenseRepository.save(license);

        saveHistory(saved.getId(), ownerId, "CREATED");

        return saved;
    }

    @Transactional
    public TicketResponse activateLicense(String code, UUID userId, UUID deviceId,
                                          String deviceName, String macAddress) {
        License license = licenseRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("License not found"));

        if (Boolean.TRUE.equals(license.getBlocked())) {
            throw new RuntimeException("License is blocked");
        }
        if (!license.getEndingDate().isAfter(LocalDate.now())) {
            throw new RuntimeException("License expired");
        }

        boolean deviceLicenseExists = deviceLicenseRepository
                .findByLicenseIdAndDeviceId(license.getId(), deviceId)
                .isPresent();

        if (!deviceLicenseExists) {
            long activatedDevices = deviceLicenseRepository.countByLicenseId(license.getId());
            if (activatedDevices >= license.getDeviceCount()) {
                throw new RuntimeException("Device limit exceeded");
            }

            if (!deviceRepository.existsById(deviceId)) {
                Device device = new Device();
                device.setId(deviceId);
                device.setUserId(userId);
                device.setName(deviceName);
                device.setMacAddress(macAddress);
                deviceRepository.save(device);
            }

            DeviceLicense deviceLicense = new DeviceLicense();
            deviceLicense.setLicenseId(license.getId());
            deviceLicense.setDeviceId(deviceId);
            deviceLicense.setActivationDate(LocalDate.now());
            deviceLicenseRepository.save(deviceLicense);
        }

        if (license.getFirstActivationDate() == null) {
            license.setFirstActivationDate(LocalDate.now());
            license.setUserId(userId);
            licenseRepository.save(license);
        }

        saveHistory(license.getId(), userId, "ACTIVATED");

        return buildTicketResponse(license, userId, deviceId);
    }

    public TicketResponse checkLicense(String code, UUID deviceId) {
        License license = licenseRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("License not found"));

        deviceLicenseRepository.findByLicenseIdAndDeviceId(license.getId(), deviceId)
                .orElseThrow(() -> new RuntimeException("Device is not activated for this license"));

        if (Boolean.TRUE.equals(license.getBlocked())) {
            throw new RuntimeException("License is blocked");
        }
        if (!license.getEndingDate().isAfter(LocalDate.now())) {
            throw new RuntimeException("License expired");
        }

        return buildTicketResponse(license, license.getUserId(), deviceId);
    }

    @Transactional
    public License renewLicense(UUID licenseId, UUID userId) {
        License license = licenseRepository.findById(licenseId)
                .orElseThrow(() -> new RuntimeException("License not found"));

        LicenseType type = licenseTypeRepository.findById(license.getTypeId())
                .orElseThrow(() -> new RuntimeException("License type not found"));

        LocalDate renewalBase = license.getEndingDate().isAfter(LocalDate.now())
                ? license.getEndingDate()
                : LocalDate.now();
        license.setEndingDate(renewalBase.plusDays(type.getDefaultDurationInDays()));
        License saved = licenseRepository.save(license);

        saveHistory(licenseId, userId, "RENEWED");

        return saved;
    }

    private void saveHistory(UUID licenseId, UUID userId, String status) {
        LicenseHistory history = new LicenseHistory();
        history.setLicenseId(licenseId);
        history.setUserId(userId);
        history.setStatus(status);
        history.setChangeDate(LocalDateTime.now());
        licenseHistoryRepository.save(history);
    }

    private TicketResponse buildTicketResponse(License license, UUID userId, UUID deviceId) {
        Ticket ticket = new Ticket(
                LocalDateTime.now(),
                license.getFirstActivationDate(),
                license.getEndingDate(),
                userId,
                deviceId,
                license.getBlocked()
        );

        try {
            String signature = digitalSignatureService.sign(ticket);
            TicketResponse response = new TicketResponse();
            response.setTicket(ticket);
            response.setDigitalSignature(signature);
            return response;
        } catch (Exception e) {
            throw new IllegalStateException("Ticket signing failed", e);
        }
    }
}
