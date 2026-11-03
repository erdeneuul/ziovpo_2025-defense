package ru.mfa.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ru.mfa.dto.TicketResponse;
import ru.mfa.model.DeviceLicense;
import ru.mfa.model.License;
import ru.mfa.repository.*;
import ru.mfa.signature.DigitalSignatureService;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LicenseServiceTest {

    @Mock LicenseRepository licenseRepository;
    @Mock ProductRepository productRepository;
    @Mock LicenseTypeRepository licenseTypeRepository;
    @Mock DeviceRepository deviceRepository;
    @Mock DeviceLicenseRepository deviceLicenseRepository;
    @Mock LicenseHistoryRepository licenseHistoryRepository;
    @Mock DigitalSignatureService digitalSignatureService;

    private LicenseService service;

    @BeforeEach
    void setUp() {
        service = new LicenseService(licenseRepository, productRepository, licenseTypeRepository, deviceRepository,
                deviceLicenseRepository, licenseHistoryRepository, digitalSignatureService);
    }

    @Test
    void existingActivatedDeviceCanRequestTicketWhenLimitIsReached() throws Exception {
        UUID licenseId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        License license = new License();
        ReflectionTestUtils.setField(license, "id", licenseId);
        license.setCode("LIC-1");
        license.setDeviceCount(1);
        license.setBlocked(false);
        license.setFirstActivationDate(LocalDate.now().minusDays(1));
        license.setEndingDate(LocalDate.now().plusDays(30));
        license.setUserId(userId);

        when(licenseRepository.findByCode("LIC-1")).thenReturn(Optional.of(license));
        when(deviceLicenseRepository.findByLicenseIdAndDeviceId(licenseId, deviceId))
                .thenReturn(Optional.of(new DeviceLicense()));
        when(digitalSignatureService.sign(any())).thenReturn("signature");

        TicketResponse response = service.activateLicense(
                "LIC-1", userId, deviceId, "laptop", "00:11:22:33:44:55");

        assertThat(response.getDigitalSignature()).isEqualTo("signature");
        assertThat(response.getTicket().getDeviceId()).isEqualTo(deviceId);
        verify(deviceLicenseRepository, never()).countByLicenseId(any());
        verify(deviceRepository, never()).save(any());
    }
}
