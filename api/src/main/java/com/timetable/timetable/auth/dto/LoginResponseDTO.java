package com.timetable.timetable.auth.dto;

/**
 * Data Transfer Object for successful login responses, including tokens.
 *
 * @param user         The authenticated user's profile information.
 * @param accessToken  The JWT access token.
 * @param refreshToken The refresh token.
 */
public record LoginResponseDTO(
        AuthenticationResponseDTO user,
        String accessToken,
        String refreshToken) {
}
