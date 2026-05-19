package com.timetable.timetable.auth.entity;

import java.time.LocalDateTime;

import com.timetable.timetable.domain.user.entity.ApplicationUser;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing a Refresh Token used for maintaining user sessions.
 *
 * <p>
 * Each token is tied to a specific user and contains metadata about the
 * client session such as IP address and User-Agent.
 * </p>
 */
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String token;

    private String ip;

    private String userAgent;

    private LocalDateTime createdAt;

    private LocalDateTime expiresAt;

    private boolean revoked;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id")
    private ApplicationUser user;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    public boolean isActive() {
        return !revoked && !isExpired();
    }
}
