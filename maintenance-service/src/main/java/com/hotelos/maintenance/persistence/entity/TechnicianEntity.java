package com.hotelos.maintenance.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "technicians", schema = "maintenance")
public class TechnicianEntity {

    @Id
    @Column(name = "name", length = 64, nullable = false)
    private String name;

    @Column(name = "state", length = 32, nullable = false)
    private String state;

    @Column(name = "availability_sequence")
    private Long availabilitySequence;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TechnicianEntity() {
    }

    public TechnicianEntity(String name, String state, Long availabilitySequence, Instant createdAt, Instant updatedAt) {
        this.name = name;
        this.state = state;
        this.availabilitySequence = availabilitySequence;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
    }

    public String getName() {
        return name;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Long getAvailabilitySequence() {
        return availabilitySequence;
    }

    public void setAvailabilitySequence(Long availabilitySequence) {
        this.availabilitySequence = availabilitySequence;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
