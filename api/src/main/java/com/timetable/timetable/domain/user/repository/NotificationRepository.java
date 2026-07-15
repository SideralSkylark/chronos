package com.timetable.timetable.domain.user.repository;

import com.timetable.timetable.domain.user.entity.Notification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

  List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

  long countByUserIdAndReadFlagFalse(Long userId);

  @Modifying
  @Transactional
  @Query("UPDATE Notification n SET n.readFlag = true WHERE n.user.id = :userId")
  void markAllReadByUserId(@Param("userId") Long userId);

  @Modifying
  @Transactional
  @Query("UPDATE Notification n SET n.readFlag = true WHERE n.id = :id AND n.user.id = :userId")
  void markAsRead(@Param("id") Long id, @Param("userId") Long userId);

  @Modifying
  @Transactional
  @Query("DELETE FROM Notification n WHERE n.user.id = :userId AND n.readFlag = true")
  void deleteReadByUserId(@Param("userId") Long userId);

  @Query("SELECT COUNT(n) > 0 FROM Notification n WHERE n.id = :id AND n.user.id = :userId")
  boolean existsByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
