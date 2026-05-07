package com.timetable.timetable.domain.user.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.timetable.timetable.domain.user.entity.ApplicationUser;
import com.timetable.timetable.domain.user.entity.UserRole;

@Repository
public interface UserRepository
        extends JpaRepository<ApplicationUser, Long>, JpaSpecificationExecutor<ApplicationUser> {

    Optional<ApplicationUser> findByUsername(String username);

    Optional<ApplicationUser> findByEmail(String email);

    @Query("SELECT u FROM ApplicationUser u JOIN u.roles r WHERE u.id = :id AND r.role = :role")
    Optional<ApplicationUser> findByIdAndRole(@Param("id") Long id, @Param("role") UserRole role);

    boolean existsByIdAndRole(Long id, UserRole role);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM ApplicationUser u JOIN u.roles r WHERE r.role = :role")
    List<ApplicationUser> findAllByRole(@Param("role") UserRole role);

    @Query("SELECT u FROM ApplicationUser u JOIN u.roles r WHERE r.role = :role")
    Page<ApplicationUser> findAllByRole(@Param("role") UserRole role, Pageable pageable);

    @Query("SELECT u FROM ApplicationUser u WHERE :role NOT IN (SELECT r.role FROM u.roles r)")
    List<ApplicationUser> findAllExcludingRole(@Param("role") UserRole role);

    @Query("SELECT u FROM ApplicationUser u WHERE :role NOT IN (SELECT r.role FROM u.roles r)")
    Page<ApplicationUser> findAllExcludingRole(@Param("role") UserRole role, Pageable pageable);

    @Query("SELECT COUNT(u) FROM ApplicationUser u JOIN u.roles r WHERE r.role = :role")
    long countUsersByRole(@Param("role") UserRole role);

    @Query("SELECT COUNT(u) > 0 FROM ApplicationUser u WHERE u.username = :username AND u.id <> :id")
    boolean existsByUsernameAndIdNot(@Param("username") String username, @Param("id") Long id);

    @Query("SELECT COUNT(u) > 0 FROM ApplicationUser u WHERE u.email = :email AND u.id <> :id")
    boolean existsByEmailAndIdNot(@Param("email") String email, @Param("id") Long id);
}
