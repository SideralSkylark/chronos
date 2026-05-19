package com.timetable.timetable.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.timetable.timetable.auth.dto.AuthenticationResponseDTO;
import com.timetable.timetable.auth.dto.LoginRequestDTO;
import com.timetable.timetable.auth.dto.SessionDTO;
import com.timetable.timetable.auth.entity.RefreshToken;
import com.timetable.timetable.auth.exception.InvalidCredentialsException;
import com.timetable.timetable.auth.exception.InvalidTokenException;
import com.timetable.timetable.auth.mapper.SessionMapper;
import com.timetable.timetable.auth.service.AuthenticationService;
import com.timetable.timetable.auth.service.RefreshTokenService;
import com.timetable.timetable.auth.util.CookieUtil;
import com.timetable.timetable.domain.user.entity.AccountStatus;
import com.timetable.timetable.domain.user.entity.ApplicationUser;
import com.timetable.timetable.domain.user.entity.UserRole;
import com.timetable.timetable.domain.user.entity.UserRoleEntity;
import com.timetable.timetable.domain.user.exception.UserNotFoundException;
import com.timetable.timetable.domain.user.repository.UserRepository;
import com.timetable.timetable.security.JwtService;
import com.timetable.timetable.security.SecurityUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.List;

import org.mockito.MockedStatic;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private CookieUtil cookieUtil;
    @Mock
    private SessionMapper sessionMapper;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private AuthenticationService authenticationService;

    private ApplicationUser testUser;
    private LoginRequestDTO loginRequest;

    @BeforeEach
    void setUp() {
        UserRoleEntity role = new UserRoleEntity();
        role.setRole(UserRole.ADMIN);

        testUser = ApplicationUser.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .status(AccountStatus.ACTIVE)
                .roles(Set.of(role))
                .build();

        loginRequest = new LoginRequestDTO("test@example.com", "password");
    }

    @Test
    void login_Success() {
        when(userRepository.findByEmail(loginRequest.email())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(loginRequest.password(), testUser.getPassword())).thenReturn(true);
        when(jwtService.generateToken(testUser)).thenReturn("accessToken");
        
        RefreshToken refreshTokenEntity = new RefreshToken();
        refreshTokenEntity.setToken("refreshToken");
        when(refreshTokenService.createRefreshToken(eq(testUser), eq(request))).thenReturn(refreshTokenEntity);

        AuthenticationResponseDTO result = authenticationService.login(loginRequest, request, response);

        assertThat(result).isNotNull();
        assertThat(result.email()).isEqualTo(testUser.getEmail());
        verify(cookieUtil).setTokenCookie(eq(response), eq(CookieUtil.ACCESS_TOKEN_COOKIE), eq("accessToken"), any());
        verify(cookieUtil).setTokenCookie(eq(response), eq(CookieUtil.REFRESH_TOKEN_COOKIE), eq("refreshToken"), any());
    }

    @Test
    void login_InvalidUser_ThrowsInvalidCredentialsException() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.login(loginRequest, request, response))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_InvalidPassword_ThrowsInvalidCredentialsException() {
        when(userRepository.findByEmail(loginRequest.email())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authenticationService.login(loginRequest, request, response))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_UserNotEnabled_ThrowsUserNotFoundException() {
        testUser.setStatus(AccountStatus.INACTIVE);
        when(userRepository.findByEmail(loginRequest.email())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(loginRequest.password(), testUser.getPassword())).thenReturn(true);

        assertThatThrownBy(() -> authenticationService.login(loginRequest, request, response))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void refreshAccessToken_Success() {
        String oldRefreshToken = "oldRefreshToken";
        when(cookieUtil.extractTokenFromCookie(request, CookieUtil.REFRESH_TOKEN_COOKIE)).thenReturn(oldRefreshToken);
        when(refreshTokenService.isTokenValid(oldRefreshToken)).thenReturn(true);
        when(refreshTokenService.getUserFromToken(oldRefreshToken)).thenReturn(testUser);
        when(jwtService.generateToken(testUser)).thenReturn("newAccessToken");
        
        RefreshToken newRefreshTokenEntity = new RefreshToken();
        newRefreshTokenEntity.setToken("newRefreshToken");
        when(refreshTokenService.createRefreshToken(eq(testUser), eq(request))).thenReturn(newRefreshTokenEntity);

        authenticationService.refreshAccessToken(request, response);

        verify(cookieUtil).setTokenCookie(eq(response), eq(CookieUtil.ACCESS_TOKEN_COOKIE), eq("newAccessToken"), any());
        verify(cookieUtil).setTokenCookie(eq(response), eq(CookieUtil.REFRESH_TOKEN_COOKIE), eq("newRefreshToken"), any());
    }

    @Test
    void refreshAccessToken_InvalidToken_ThrowsInvalidTokenException() {
        when(cookieUtil.extractTokenFromCookie(any(), anyString())).thenReturn("invalidToken");
        when(refreshTokenService.isTokenValid(anyString())).thenReturn(false);

        assertThatThrownBy(() -> authenticationService.refreshAccessToken(request, response))
                .isInstanceOf(InvalidTokenException.class);
        
        verify(cookieUtil).clearCookie(eq(response), eq(CookieUtil.ACCESS_TOKEN_COOKIE));
        verify(cookieUtil).clearCookie(eq(response), eq(CookieUtil.REFRESH_TOKEN_COOKIE));
    }

    @Test
    void logout_Success() {
        String refreshToken = "someToken";
        when(cookieUtil.extractTokenFromCookie(request, CookieUtil.REFRESH_TOKEN_COOKIE)).thenReturn(refreshToken);

        authenticationService.logout(request, response);

        verify(refreshTokenService).deleteByToken(refreshToken);
        verify(cookieUtil).clearCookie(response, CookieUtil.ACCESS_TOKEN_COOKIE);
        verify(cookieUtil).clearCookie(response, CookieUtil.REFRESH_TOKEN_COOKIE);
    }

    @Test
    void listSessions_Success() {
        String username = "testuser";
        Pageable pageable = mock(Pageable.class);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));
        
        RefreshToken token = new RefreshToken();
        Page<RefreshToken> tokenPage = new PageImpl<>(List.of(token));
        when(refreshTokenService.findAllByUserId(testUser.getId(), pageable)).thenReturn(tokenPage);
        
        SessionDTO sessionDTO = new SessionDTO(1L, "ip", "device", null, true);
        when(sessionMapper.toSessionDTO(token)).thenReturn(sessionDTO);

        Page<SessionDTO> result = authenticationService.listSessions(username, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(sessionDTO);
    }

    @Test
    void logoutWithToken_Success() {
        Long tokenId = 1L;
        RefreshToken tokenEntity = new RefreshToken();
        tokenEntity.setToken("tokenValue");
        tokenEntity.setUser(testUser);
        
        when(refreshTokenService.findByTokenId(tokenId)).thenReturn(Optional.of(tokenEntity));
        
        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getAuthenticatedUsername).thenReturn(testUser.getUsername());
            
            authenticationService.logoutWithToken(tokenId);
            
            verify(refreshTokenService).deleteByToken("tokenValue");
        }
    }

    @Test
    void logoutWithToken_Unauthorized_ThrowsInvalidTokenException() {
        Long tokenId = 1L;
        RefreshToken tokenEntity = new RefreshToken();
        ApplicationUser otherUser = ApplicationUser.builder().username("other").build();
        tokenEntity.setUser(otherUser);
        
        when(refreshTokenService.findByTokenId(tokenId)).thenReturn(Optional.of(tokenEntity));
        
        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getAuthenticatedUsername).thenReturn(testUser.getUsername());
            
            assertThatThrownBy(() -> authenticationService.logoutWithToken(tokenId))
                    .isInstanceOf(InvalidTokenException.class);
        }
    }
}
