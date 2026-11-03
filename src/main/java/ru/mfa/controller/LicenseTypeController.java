package ru.mfa.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.mfa.model.LicenseType;
import ru.mfa.repository.LicenseTypeRepository;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/license-types")
public class LicenseTypeController {

    private final LicenseTypeRepository licenseTypeRepository;

    public LicenseTypeController(LicenseTypeRepository licenseTypeRepository) {
        this.licenseTypeRepository = licenseTypeRepository;
    }

    @GetMapping
    public List<LicenseType> getAll() {
        return licenseTypeRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<LicenseType> getById(@PathVariable UUID id) {
        return licenseTypeRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LicenseType> create(@RequestBody LicenseType licenseType) {
        return ResponseEntity.ok(licenseTypeRepository.save(licenseType));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LicenseType> update(@PathVariable UUID id, @RequestBody LicenseType updated) {
        return licenseTypeRepository.findById(id)
                .map(lt -> {
                    lt.setName(updated.getName());
                    lt.setDefaultDurationInDays(updated.getDefaultDurationInDays());
                    lt.setDescription(updated.getDescription());
                    return ResponseEntity.ok(licenseTypeRepository.save(lt));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (!licenseTypeRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        licenseTypeRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
