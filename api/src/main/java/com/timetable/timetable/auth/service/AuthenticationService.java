package com.timetable.timetable.auth.service;

import com.timetable.timetable.auth.dto.AuthenticationResponseDTO;
import com.timetable.timetable.auth.dto.LoginRequestDTO;
import com.timetable.timetable.auth.dto.SessionDTO;
import com.timetable.timetable.auth.entity.RefreshToken;
import com.timetable.timetable.auth.exception.InvalidCredentialsException;
import com.timetable.timetable.auth.exception.InvalidTokenException;
import com.timetable.timetable.auth.mapper.SessionMapper;
import com.timetable.timetable.auth.util.CookieUtil;
import com.timetable.timetable.auth.util.CookieUtil.TokenType;
import com.timetable.timetable.domain.user.entity.ApplicationUser;
import com.timetable.timetable.domain.user.exception.UserNotFoundException;
import com.timetable.timetable.domain.user.repository.UserRepository;
import com.timetable.timetable.security.JwtService;
import com.timetable.timetable.security.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AuthenticationService
 *
 * <p>This service manages the core authentication flow of the application. It provides
 * functionality for:
 *
 * <ul>
 *   <li>User login (authentication) and token issuance
 *   <li>JWT refresh token lifecycle handling
 *   <li>User logout and session revocation
 *   <li>Session listing and targeted session invalidation
 * </ul>
 *
 * <p>Collaborates with:
 *
 * <ul>
 *   <li>{@link JwtService} – for generating access tokens
 *   <li>{@link RefreshTokenService} – for refresh token lifecycle management
 *   <li>{@link CookieUtil} – for secure cookie handling
 *   <li>{@link SessionMapper} – for transforming session entities to DTOs
 * </ul>
 *
 * @author Sideral Skylark
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

  private static final String ACCESS_COOKIE = CookieUtil.ACCESS_TOKEN_COOKIE;
  private static final String REFRESH_COOKIE = CookieUtil.REFRESH_TOKEN_COOKIE;

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final RefreshTokenService refreshTokenService;
  private final CookieUtil cookieUtil;
  private final SessionMapper sessionMapper;

  /**
   * Authenticates a user and returns a JWT token upon successful login.
   *
   * <p>The method checks:
   *
   * <ul>
   *   <li>That the email exists in the system
   *   <li>That the password is correct
   *   <li>That the user account is enabled
   * </ul>
   *
   * @param loginRequestDTO DTO containing the user's email and password
   * @param request to extract client data to be persisted
   * @param response to bind the generated tokens to a cookie
   * @return an {@link AuthenticationResponseDTO} with the user details
   * @throws InvalidCredentialsException if credentials are invalid
   * @throws UserNotFoundException if the account is not verified
   */
  @Transactional
  public AuthenticationResponseDTO login(
      LoginRequestDTO loginRequestDTO, HttpServletRequest request, HttpServletResponse response) {
    log.debug("User attempting login with email: {}", loginRequestDTO.email());

    ApplicationUser user =
        userRepository
            .findByEmail(loginRequestDTO.email())
            .orElseThrow(
                () -> {
                  log.warn("Login failed: user not found - {}", loginRequestDTO.email());
                  return new InvalidCredentialsException("Invalid credentials");
                });

    if (!passwordEncoder.matches(loginRequestDTO.password(), user.getPassword())) {
      log.warn("Login failed: invalid password for email - {}", loginRequestDTO.email());
      throw new InvalidCredentialsException("Invalid credentials");
    }

    if (!user.isEnabled()) {
      log.warn("Login failed: account not verified for email - {}", loginRequestDTO.email());
      throw new UserNotFoundException("Account not verified. Please verify your email first.");
    }

    issueTokens(user, response, request);

    log.info("Login successful for user: {}", user.getId());
    return buildAuthenticationResponse(user);
  }

  /**
   * Refreshes the user's access token using a valid refresh token from the HTTP cookie.
   *
   * <p>If the refresh token is valid and not expired, new tokens (access and refresh) are issued
   * and stored in cookies.
   *
   * @param request the HTTP request containing the refresh token cookie
   * @param response the HTTP response where new cookies will be set
   * @throws InvalidTokenException if the refresh token is missing, invalid, or expired
   */
  public void refreshAccessToken(HttpServletRequest request, HttpServletResponse response) {
    log.debug("Refreshing access token");
    String token = cookieUtil.extractTokenFromCookie(request, REFRESH_COOKIE);

    if (token == null || !refreshTokenService.isTokenValid(token)) {
      log.warn("Invalid refresh token in refresh request");
      cookieUtil.clearCookie(response, ACCESS_COOKIE);
      cookieUtil.clearCookie(response, REFRESH_COOKIE);
      throw new InvalidTokenException("Invalid or expired refresh token");
    }

    ApplicationUser user = refreshTokenService.getUserFromToken(token);
    issueTokens(user, response, request);

    log.info("Refreshed tokens for user: {}", user.getId());
  }

  /**
   * Logs out the current user by revoking their refresh token and clearing authentication cookies.
   *
   * @param request the HTTP request containing the refresh token cookie
   * @param response the HTTP response used to clear the cookies
   */
  @Transactional
  public void logout(HttpServletRequest request, HttpServletResponse response) {
    log.debug("Attempting to log out");
    String refreshToken = cookieUtil.extractTokenFromCookie(request, REFRESH_COOKIE);
    if (refreshToken != null) {
      refreshTokenService.deleteByToken(refreshToken);
    }
    cookieUtil.clearCookie(response, CookieUtil.ACCESS_TOKEN_COOKIE);
    cookieUtil.clearCookie(response, CookieUtil.REFRESH_TOKEN_COOKIE);
    log.info("User logged out.");
  }

  /**
   * Lists all active sessions for the specified user.
   *
   * <p>Returns paginated session data using tokens tied to the user.
   *
   * @param username the username of the user
   * @param pageable pagination information
   * @return a page of {@link SessionDTO} representing active sessions
   * @throws UserNotFoundException if the user is not found
   */
  public Page<SessionDTO> listSessions(String username, Pageable pageable) {
    log.debug("Listing active sessions for user {}", username);
    ApplicationUser user = findUserByUsernameOrThrow(username);
    return refreshTokenService
        .findAllByUserId(user.getId(), pageable)
        .map(sessionMapper::toSessionDTO);
  }

  /**
   * Logs out (invalidates) a single session using the token ID.
   *
   * <p>Only allows the authenticated user to invalidate their own session token.
   *
   * @param tokenId the ID of the refresh token to invalidate
   * @throws InvalidTokenException if the token does not exist or doesn't belong to the user
   */
  @Transactional
  public void logoutWithToken(Long tokenId) {
    log.debug("Attempting remote log-out for token-id: {}", tokenId);
    RefreshToken token =
        refreshTokenService
            .findByTokenId(tokenId)
            .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

    String currentUsername = SecurityUtil.getAuthenticatedUsername();
    if (!token.getUser().getUsername().equals(currentUsername)) {
      log.error("Unauthorized session invalidation attempt by: {}", currentUsername);
      throw new InvalidTokenException("You are not authorized to invalidate this session.");
    }

    refreshTokenService.deleteByToken(token.getToken());
    log.info("Session invalidated for token: {}", tokenId);
  }

  /**
   * Issues new access and refresh tokens for the given user and stores them in cookies.
   *
   * <p>Uses {@link JwtService} to generate the access token and {@link RefreshTokenService} for
   * refresh token generation.
   *
   * @param user the authenticated user
   * @param response the HTTP response used to set cookies
   * @param request the HTTP request used for IP and user agent info
   */
  private void issueTokens(
      ApplicationUser user, HttpServletResponse response, HttpServletRequest request) {
    log.debug("Issueing tokens for user: {}", user.getId());
    String accessToken = jwtService.generateToken(user);
    String refreshToken = refreshTokenService.createRefreshToken(user, request).getToken();

    cookieUtil.setTokenCookie(response, ACCESS_COOKIE, accessToken, TokenType.ACCESS);
    cookieUtil.setTokenCookie(response, REFRESH_COOKIE, refreshToken, TokenType.REFRESH);
  }

  /**
   * Builds an {@link AuthenticationResponseDTO} from the given user and JWT token.
   *
   * @param user the authenticated user
   * @return a response DTO containing user information and token
   */
  private AuthenticationResponseDTO buildAuthenticationResponse(ApplicationUser user) {
    log.debug("Building authentication response for user: {}", user.getEmail());

    AuthenticationResponseDTO response =
        new AuthenticationResponseDTO(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getRoles().stream().map(role -> role.getRole().name()).collect(Collectors.toSet()),
            user.getUpdatedAt());
    return response;
  }

  /**
   * Retrieves a user by their username or throws a {@link UserNotFoundException} if not found.
   *
   * <p>The username is expected to be already normalized (lowercased and trimmed) by the caller. If
   * the user does not exist, a warning is logged and a {@code UserNotFoundException} is thrown.
   *
   * @param username The normalized username of the user to retrieve
   * @return The {@link ApplicationUser} corresponding to the given username
   * @throws UserNotFoundException if no user is found with the specified username
   */
  private ApplicationUser findUserByUsernameOrThrow(String username) {
    log.debug("Querying user for username: {}", username);
    return userRepository
        .findByUsername(username)
        .orElseThrow(
            () -> {
              log.warn("User not found for username: {}", username);
              return new UserNotFoundException("User not found");
            });
  }
}
