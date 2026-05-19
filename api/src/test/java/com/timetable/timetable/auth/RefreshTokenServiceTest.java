package com.timetable.timetable.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.timetable.timetable.auth.entity.RefreshToken;
import com.timetable.timetable.auth.exception.InvalidTokenException;
import com.timetable.timetable.auth.repository.RefreshTokenRepository;
import com.timetable.timetable.auth.service.RefreshTokenService;
import com.timetable.timetable.domain.user.entity.ApplicationUser;

import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private ApplicationUser testUser;
    private RefreshToken testToken;

    @BeforeEach
    void setUp() {
        testUser = ApplicationUser.builder()
                .id(1L)
                .username("testuser")
                .build();

        testToken = RefreshToken.builder()
                .id(1L)
                .token("valid-token")
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();
    }

    @Test
    void createRefreshToken_Success() {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken result = refreshTokenService.createRefreshToken(testUser, request);

        assertThat(result).isNotNull();
        assertThat(result.getUser()).isEqualTo(testUser);
        assertThat(result.getIp()).isEqualTo("127.0.0.1");
        assertThat(result.getUserAgent()).isEqualTo("Mozilla/5.0");
        assertThat(result.getToken()).isNotNull();
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void isTokenValid_ValidToken_ReturnsTrue() {
        when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(testToken));

        boolean result = refreshTokenService.isTokenValid("valid-token");

        assertThat(result).isTrue();
    }

    @Test
    void isTokenValid_ExpiredToken_ReturnsFalse() {
        testToken.setExpiresAt(LocalDateTime.now().minusDays(1));
        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(testToken));

        boolean result = refreshTokenService.isTokenValid("expired-token");

        assertThat(result).isFalse();
    }

    @Test
    void isTokenValid_RevokedToken_ReturnsFalse() {
        testToken.setRevoked(true);
        when(refreshTokenRepository.findByToken("revoked-token")).thenReturn(Optional.of(testToken));

        boolean result = refreshTokenService.isTokenValid("revoked-token");

        assertThat(result).isFalse();
    }

    @Test
    void getUserFromToken_Success() {
        when(refreshTokenRepository.findByTokenWithUser("valid-token")).thenReturn(Optional.of(testToken));

        ApplicationUser result = refreshTokenService.getUserFromToken("valid-token");

        assertThat(result).isEqualTo(testUser);
    }

    @Test
    void rotateRefreshToken_Success() {
        when(refreshTokenRepository.findByTokenWithUser("old-token")).thenReturn(Optional.of(testToken));
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String newToken = refreshTokenService.rotateRefreshToken("old-token", request);

        assertThat(newToken).isNotNull();
        assertThat(testToken.isRevoked()).isTrue();
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    void rotateRefreshToken_InvalidToken_ThrowsException() {
        when(refreshTokenRepository.findByTokenWithUser("invalid-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken("invalid-token", request))
                .isInstanceOf(InvalidTokenException.class);
    }
}
