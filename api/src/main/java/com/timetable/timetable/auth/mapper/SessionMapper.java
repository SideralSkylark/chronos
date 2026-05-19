package com.timetable.timetable.auth.mapper;

import org.springframework.stereotype.Component;

import com.timetable.timetable.auth.dto.SessionDTO;
import com.timetable.timetable.auth.entity.RefreshToken;

/**
 * Mapper class to convert {@link RefreshToken} entities to {@link SessionDTO}s.
 */
@Component
public class SessionMapper {
    /**
     * Maps a {@link RefreshToken} entity to a {@link SessionDTO}.
     *
     * @param token The refresh token entity to map.
     * @return The corresponding session DTO.
     */
    public SessionDTO toSessionDTO(RefreshToken token) {
        SessionDTO dto = new SessionDTO(
                token.getId(),
                token.getIp(),
                token.getUserAgent(),
                token.getCreatedAt(),
                token.isActive());
        return dto;
    }
}
