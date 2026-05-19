package com.timetable.timetable.domain.user.entity;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import com.timetable.timetable.domain.schedule.entity.TeacherType;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity representing a user in the system.
 *
 * <p>Implements {@link UserDetails} for Spring Security integration.
 * Supports multiple roles, account status management, and teacher-specific attributes.</p>
 *
 * @author Sideral Skylark
 */
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users")
@ToString(exclude = "password")
public class ApplicationUser implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50, unique = true)
    private String username;

    @Column(nullable = false, length = 100, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AccountStatus status = AccountStatus.INACTIVE;

    @Builder.Default
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<UserRoleEntity> roles = new HashSet<>();

    @Column(nullable = false)
    @Builder.Default
    private boolean simulationTeam = false;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private TeacherType teacherType = null; // when not a teacher

    // ====== DOMAIN LOGIC ======

    /**
     * Adds a role to the user.
     * @param role the role entity to add
     * @return true if the role was added, false if already present
     */
    public boolean addRole(UserRoleEntity role) {
        return roles.add(role);
    }

    /**
     * Removes a role from the user.
     * @param role the role entity to remove
     * @return true if the role was removed
     */
    public boolean removeRole(UserRoleEntity role) {
        return roles.remove(role);
    }

    /**
     * Checks if the user has a specific role.
     * @param role the role to check
     * @return true if the user has the role
     */
    public boolean hasRole(UserRole role) {
        return roles.stream().anyMatch(r -> r.getRole() == role);
    }

    /**
     * Checks if the user lacks a specific role.
     * @param role the role to check
     * @return true if the user does not have the role
     */
    public boolean lacksRole(UserRole role) {
        return roles.stream().noneMatch(r -> r.getRole() == role);
    }

    /**
     * @return true if the user has the ADMIN role
     */
    public boolean isAdmin() {
        return hasRole(UserRole.ADMIN);
    }

    /**
     * @return true if the user only has the USER role
     */
    public boolean isUser() {
        return roles.size() == 1 && hasRole(UserRole.USER);
    }

    /**
     * Gets the role with the highest priority.
     * @return the highest privilege role
     */
    public UserRole getHighestPrivilegeRole() {
        return roles.stream()
                .map(UserRoleEntity::getRole)
                .max(Comparator.comparingInt(UserRole::getPriority))
                .orElse(UserRole.USER);
    }

    /**
     * Activates the user account.
     */
    public void activate() {
        this.status = AccountStatus.ACTIVE;
    }

    /**
     * Deactivates the user account.
     */
    public void deactivate() {
        this.status = AccountStatus.INACTIVE;
    }

    // ====== SPRING SECURITY ======

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> (GrantedAuthority) () -> "ROLE_" + role.getRole().name())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public boolean isAccountNonExpired() {
        return (status != null) ? true : false;
    }

    @Override
    public boolean isAccountNonLocked() {
        return (status != null) ? true : false;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == AccountStatus.ACTIVE;
    }

    // ====== FACTORY ======

    /**
     * Creates a new user with INACTIVE status and no roles.
     * @param username the username
     * @param email the email
     * @param password the encoded password
     * @return a new ApplicationUser instance
     */
    public static ApplicationUser createUser(String username, String email, String password) {
        return ApplicationUser.builder()
                .username(username)
                .email(email)
                .password(password)
                .status(AccountStatus.INACTIVE)
                .roles(new HashSet<>())
                .build();
    }

    // ====== EQUALITY ======

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ApplicationUser user))
            return false;
        return Objects.equals(email, user.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email);
    }

    // ===== Helper ========
    /**
     * Returns the maximum weekly hours allowed for this user if they are a teacher.
     * @return the weekly hours limit
     */
    public int getWeeklyHoursLimit() {
        if (simulationTeam)
            return 999;
            
        // Phantoms are typically temporary/placeholder teachers
        if (username != null && username.startsWith("PHANTOM_")) {
            return 8; // Default to PART_TIME limit
        }

        if (teacherType == null)
            return 0;
            
        return switch (teacherType) {
            case FULL_TIME -> 24;
            case PART_TIME -> 8;
        };
    }
}
