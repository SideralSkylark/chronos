package com.timetable.timetable.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.timetable.timetable.auth.exception.UserNotAuthorizedException;
import com.timetable.timetable.domain.user.dto.NotificationDto;
import com.timetable.timetable.domain.user.entity.ApplicationUser;
import com.timetable.timetable.domain.user.entity.Notification;
import com.timetable.timetable.domain.user.entity.UserRole;
import com.timetable.timetable.domain.user.entity.UserRoleEntity;
import com.timetable.timetable.domain.user.repository.NotificationRepository;
import com.timetable.timetable.domain.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationService notificationService;

    private ApplicationUser testUser;
    private Notification testNotification;

    @BeforeEach
    void setUp() {
        testUser = ApplicationUser.builder()
                .id(1L)
                .username("testuser")
                .build();

        testNotification = Notification.builder()
                .id(1L)
                .user(testUser)
                .message("Test message")
                .readFlag(false)
                .build();
    }

    @Test
    void notify_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        
        notificationService.notify(1L, "Hello");
        
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void notifyAllWithRole_Success() {
        UserRoleEntity roleEntity = new UserRoleEntity(1L, UserRole.STUDENT);
        ApplicationUser student = ApplicationUser.builder().id(2L).roles(Set.of(roleEntity)).build();
        
        when(userRepository.findAllByRole(UserRole.STUDENT)).thenReturn(List.of(student));
        
        notificationService.notifyAllWithRole("STUDENT", "Global message", 1L);
        
        verify(notificationRepository).saveAll(anyList());
    }

    @Test
    void getForUser_Success() {
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(testNotification));
        
        List<NotificationDto> result = notificationService.getForUser(1L);
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0).message()).isEqualTo("Test message");
    }

    @Test
    void markAllRead_Success() {
        notificationService.markAllRead(1L);
        verify(notificationRepository).markAllReadByUserId(1L);
    }

    @Test
    void deleteNotification_Success() {
        when(notificationRepository.existsByIdAndUserId(1L, 1L)).thenReturn(true);
        
        notificationService.deleteNotification(1L, 1L);
        
        verify(notificationRepository).deleteById(1L);
    }

    @Test
    void deleteNotification_Unauthorized_ThrowsException() {
        when(notificationRepository.existsByIdAndUserId(1L, 2L)).thenReturn(false);
        when(notificationRepository.existsById(1L)).thenReturn(true);
        
        assertThatThrownBy(() -> notificationService.deleteNotification(1L, 2L))
                .isInstanceOf(UserNotAuthorizedException.class);
    }
}
