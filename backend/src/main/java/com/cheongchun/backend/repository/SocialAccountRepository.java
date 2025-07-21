package com.cheongchun.backend.repository;

import com.cheongchun.backend.entity.SocialAccount;
import com.cheongchun.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {
    
    @Query("SELECT sa FROM SocialAccount sa JOIN FETCH sa.user WHERE sa.provider = :provider AND sa.providerId = :providerId")
    Optional<SocialAccount> findByProviderAndProviderId(@Param("provider") SocialAccount.Provider provider, @Param("providerId") String providerId);
    
    Optional<SocialAccount> findByUserAndProvider(User user, SocialAccount.Provider provider);
}