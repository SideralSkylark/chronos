package com.timetable.timetable.domain.user.service;

import java.time.LocalDateTime;
import java.security.SecureRandom;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.timetable.timetable.auth.exception.UserAlreadyExistsException;
import com.timetable.timetable.domain.user.dto.*;
import com.timetable.timetable.domain.user.entity.*;
import com.timetable.timetable.domain.user.exception.UserNotFoundException;
import com.timetable.timetable.domain.user.mapper.UserMapper;
import com.timetable.timetable.domain.user.repository.UserRepository;
import com.timetable.timetable.domain.user.repository.UserRoleRepository;
import com.timetable.timetable.domain.user.specification.UserSpecifications;
import com.timetable.timetable.security.SecurityUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserRoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public ApplicationUser createUser(CreateUser request) {
        log.debug("Creating user");
        boolean isTeacher = request.roles().stream().anyMatch(r -> r.equalsIgnoreCase("TEACHER"));

        if (isTeacher && request.teacherType() == null) {
            throw new IllegalArgumentException("Teacher type is required for teachers");
        }

        validateUniqueUser(request);
        Set<UserRoleEntity> roles = resolveRoles(request.roles());

        ApplicationUser user = ApplicationUser.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .status(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .roles(roles)
                .teacherType(request.teacherType())
                .build();

        userRepository.save(user);
        log.info("Created new user '{}' with roles {}", user.getUsername(), roles);
        return user;
    }

    @Transactional
    public UserResponse createUserResponse(CreateUser request) {
        return userMapper.toDTO(createUser(request));
    }

    // ============================================================
    // READ USERS
    // ============================================================

    public UserResponse getAuthenticatedUserProfile() {
        ApplicationUser user = getByUsernameOrThrow(SecurityUtil.getAuthenticatedUsername());
        return userMapper.toDTO(user);
    }

    public ApplicationUser getAuthenticatedUser() {
        return getByUsernameOrThrow(SecurityUtil.getAuthenticatedUsername());
    }

    public Page<UserResponse> getAllUsers(Pageable pageable, UserFilterParams filter) {
        log.info("Filter received: {}", filter);
        return userRepository.findAll(UserSpecifications.withFilters(filter), pageable).map(userMapper::toDTO);
    }

    public Page<UserResponse> getUsersByRole(UserRole role, Pageable pageable, UserFilterParams filter) {
        return userRepository.findAll(
                UserSpecifications.withFilters(filter)
                        .and(UserSpecifications.hasRole(role)),
                pageable)
                .map(userMapper::toDTO);
    }

    public ApplicationUser findUserOrThrow(Long id) {
        return getByIdOrThrow(id);
    }

    public UserResponse getUserById(Long id) {
        return userMapper.toDTO(getByIdOrThrow(id));
    }

    public UserResponse getUserByRoleAndId(UserRole role, Long id) {
        ApplicationUser user = userRepository.findByIdAndRole(id, role)
                .orElseThrow(() -> new UserNotFoundException(
                        "No user with id %d and role %s".formatted(id, role)));

        return userMapper.toDTO(user);
    }

    public ApplicationUser getUserByUsername(String username) {
        return getByUsernameOrThrow(username);
    }

    public Optional<ApplicationUser> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public ApplicationUser getUserByEmail(String email) {
        return getByEmailOrThrow(email);
    }

    // ============================================================
    // UPDATE USERS
    // ============================================================
    @Transactional
    public UserResponse updateAuthenticatedUserProfile(UpdateUserProfileDTO dto) {
        log.debug("Updating user profile");
        ApplicationUser user = getByUsernameOrThrow(SecurityUtil.getAuthenticatedUsername());
        updateBasicFields(user, dto.username(), dto.email());
        log.info("User '{}' updated their profile", user.getUsername());
        return userMapper.toDTO(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateUserById(Long id, AdminUpdateUserDTO payload) {
        log.debug("Updating user {}", id);
        ApplicationUser user = getByIdOrThrow(id);

        validateUniqueUpdate(id, payload.username(), payload.email());

        Set<UserRoleEntity> newRoles = resolveRoles(payload.roles());
        validateAdminRemoval(user, newRoles);

        updateBasicFields(user, payload.username(), payload.email());
        user.setRoles(newRoles);
        user.setTeacherType(payload.teacherType());

        userRepository.save(user);
        log.info("Admin updated user '{}' with roles {}", id, newRoles);
        return userMapper.toDTO(user);
    }

    @Transactional
    public ResetPasswordResponse resetPassword(Long id) {
        log.debug("Resetting password for user {}", id);
        ApplicationUser user = getByIdOrThrow(id);
        String newPassword = generateRandomPassword(12);
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        log.info("Password reset for user id={}", id);
        return new ResetPasswordResponse(user.getPassword());
    }

    private String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    // ============================================================
    // DELETE USERS
    // ============================================================
    @Transactional
    public void deleteAuthenticatedUserProfile() {
        deleteByUsername(SecurityUtil.getAuthenticatedUsername());
    }

    @Transactional
    public void deleteByUsername(String username) {
        ApplicationUser user = getByUsernameOrThrow(username);

        userRepository.delete(user);
    }

    @Transactional
    public void deleteById(Long id) {
        ApplicationUser user = getByIdOrThrow(id);

        userRepository.delete(user);
    }

    // ============================================================
    // ACCOUNT STATUS
    // ============================================================
    @Transactional
    public boolean enableAccount(String email) {
        return updateAccountStatus(email, true);
    }

    @Transactional
    public boolean disableAccount(String email) {
        return updateAccountStatus(email, false);
    }

    private boolean updateAccountStatus(String email, boolean enable) {
        ApplicationUser user = getByEmailOrThrow(email);

        if (user.isEnabled() == enable)
            return false;

        if (enable)
            user.activate();
        else
            user.deactivate();

        userRepository.save(user);
        log.info("Account '{}' set to {}", email, enable ? "ENABLED" : "DISABLED");
        return true;
    }

    // ============================================================
    // HELPERS (GETTERS)
    // ============================================================
    private ApplicationUser getByIdOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User %d not found".formatted(id)));
    }

    private ApplicationUser getByUsernameOrThrow(String username) {
        return getOrThrow(username, userRepository::findByUsername,
                "No user found with username: " + username);
    }

    private ApplicationUser getByEmailOrThrow(String email) {
        return getOrThrow(email, userRepository::findByEmail,
                "No user found with email: " + email);
    }

    private <T> ApplicationUser getOrThrow(T key, Function<T, Optional<ApplicationUser>> finder, String errorMessage) {
        return finder.apply(key)
                .orElseThrow(() -> new UserNotFoundException(errorMessage));
    }

    private void updateBasicFields(ApplicationUser user, String username, String email) {
        user.setUsername(username);
        user.setEmail(email);
        user.setUpdatedAt(LocalDateTime.now());
    }

    // ============================================================
    // VALIDATION HELPERS
    // ============================================================
    private void validateUsernameAvailable(String username, Long excludeId) {
        boolean taken = (excludeId == null)
                ? userRepository.existsByUsername(username)
                : userRepository.existsByUsernameAndIdNot(username, excludeId);
        if (taken)
            throw new UserAlreadyExistsException("Username is already taken");
    }

    private void validateEmailAvailable(String email, Long excludeId) {
        boolean taken = (excludeId == null)
                ? userRepository.existsByEmail(email)
                : userRepository.existsByEmailAndIdNot(email, excludeId);
        if (taken)
            throw new UserAlreadyExistsException("Email is already in use");
    }

    private void validateUniqueUser(CreateUser request) {
        validateUsernameAvailable(request.username(), null);
        validateEmailAvailable(request.email(), null);
    }

    private void validateUniqueUpdate(Long id, String username, String email) {
        validateUsernameAvailable(username, id);
        validateEmailAvailable(email, id);
    }

    private void validateAdminRemoval(ApplicationUser user, Set<UserRoleEntity> newRoles) {
        boolean removingAdmin = user.hasRole(UserRole.ADMIN)
                && newRoles.stream().noneMatch(r -> r.getRole() == UserRole.ADMIN);

        long adminCount = userRepository.countUsersByRole(UserRole.ADMIN);

        if (removingAdmin && adminCount == 1) {
            throw new IllegalArgumentException("Cannot remove ADMIN role from the only admin user.");
        }
    }

    // ============================================================
    // ROLES HELPERS
    // ============================================================
    private Set<UserRoleEntity> resolveRoles(List<String> roleNames) {
        return roleNames.stream()
                .map(roleName -> {
                    UserRole roleEnum;
                    try {
                        roleEnum = UserRole.valueOf(roleName.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Invalid role: " + roleName);
                    }

                    return roleRepository.findByRole(roleEnum)
                            .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleEnum));
                })
                .collect(Collectors.toSet());
    }
}
