package com.cheongchun.backend.repository;

import com.cheongchun.backend.entity.User;
import com.cheongchun.backend.entity.UserAIProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAIProfileRepository extends JpaRepository<UserAIProfile, Long> {
    
    Optional<UserAIProfile> findByUser(User user);
    
    Optional<UserAIProfile> findByUserId(Long userId);
    
    void deleteByUser(User user);
    
    boolean existsByUser(User user);
}