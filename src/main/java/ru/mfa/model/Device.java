package ru.mfa.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "devices")
public class Device {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "mac_address", nullable = false)
    private String macAddress;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    public Device() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getMacAddress() { return macAddress; }
    public void setMacAddress(String macAddress) { this.macAddress = macAddress; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
}
