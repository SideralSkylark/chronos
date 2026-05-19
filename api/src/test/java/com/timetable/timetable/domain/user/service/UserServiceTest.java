package com.timetable.timetable.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.timetable.timetable.auth.exception.UserAlreadyExistsException;
import com.timetable.timetable.domain.user.dto.*;
import com.timetable.timetable.domain.user.entity.*;
import com.timetable.timetable.domain.user.exception.UserNotFoundException;
import com.timetable.timetable.domain.user.mapper.UserMapper;
import com.timetable.timetable.domain.user.repository.UserRepository;
import com.timetable.timetable.domain.user.repository.UserRoleRepository;
import com.timetable.timetable.security.SecurityUtil;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserRoleRepository roleRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private ApplicationUser testUser;
    private UserRoleEntity userRole;

    @BeforeEach
    void setUp() {
        userRole = new UserRoleEntity(1L, UserRole.USER);
        testUser = ApplicationUser.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .status(AccountStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(userRole)))
                .build();
    }

    @Test
    void createUser_Success() {
        CreateUser request = new CreateUser("newuser", "new@example.com", "password", List.of("USER"), null);
        
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(roleRepository.findByRole(UserRole.USER)).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        
        ApplicationUser result = userService.createUser(request);
        
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("newuser");
        verify(userRepository).save(any(ApplicationUser.class));
    }

    @Test
    void createUser_UsernameExists_ThrowsException() {
        CreateUser request = new CreateUser("testuser", "new@example.com", "password", List.of("USER"), null);
        when(userRepository.existsByUsername("testuser")).thenReturn(true);
        
        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(UserAlreadyExistsException.class);
    }

    @Test
    void getAuthenticatedUserProfile_Success() {
        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getAuthenticatedUsername).thenReturn("testuser");
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            
            UserResponse expectedResponse = new UserResponse(1L, "testuser", "test@example.com", Set.of("USER"), true, null);
            when(userMapper.toDTO(testUser)).thenReturn(expectedResponse);
            
            UserResponse result = userService.getAuthenticatedUserProfile();
            
            assertThat(result).isEqualTo(expectedResponse);
        }
    }

    @Test
    void getAllUsers_Success() {
        Pageable pageable = mock(Pageable.class);
        UserFilterParams filter = new UserFilterParams();
        Page<ApplicationUser> userPage = new PageImpl<>(List.of(testUser));
        
        when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(userPage);
        
        Page<UserResponse> result = userService.getAllUsers(pageable, filter);
        
        assertThat(result.getContent()).hasSize(1);
        verify(userMapper).toDTO(testUser);
    }

    @Test
    void updateUserById_Success() {
        AdminUpdateUserDTO payload = new AdminUpdateUserDTO("updated", "updated@example.com", List.of("USER"), null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByUsernameAndIdNot("updated", 1L)).thenReturn(false);
        when(userRepository.existsByEmailAndIdNot("updated@example.com", 1L)).thenReturn(false);
        when(roleRepository.findByRole(UserRole.USER)).thenReturn(Optional.of(userRole));
        
        userService.updateUserById(1L, payload);
        
        assertThat(testUser.getUsername()).isEqualTo("updated");
        assertThat(testUser.getEmail()).isEqualTo("updated@example.com");
        verify(userRepository).save(testUser);
    }

    @Test
    void deleteById_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        
        userService.deleteById(1L);
        
        verify(userRepository).delete(testUser);
    }

    @Test
    void enableAccount_Success() {
        testUser.setStatus(AccountStatus.INACTIVE);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        
        boolean changed = userService.enableAccount("test@example.com");
        
        assertThat(changed).isTrue();
        assertThat(testUser.isEnabled()).isTrue();
        verify(userRepository).save(testUser);
    }

    @Test
    void resetPassword_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(anyString())).thenReturn("newEncodedPassword");
        
        ResetPasswordResponse response = userService.resetPassword(1L);
        
        assertThat(response.temporaryPassword()).isNotNull().hasSize(12);
        assertThat(testUser.getPassword()).isEqualTo("newEncodedPassword");
        verify(userRepository).save(testUser);
    }
}
