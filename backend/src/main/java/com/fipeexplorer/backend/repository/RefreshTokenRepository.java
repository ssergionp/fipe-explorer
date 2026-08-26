package com.fipeexplorer.backend.repository;

import com.fipeexplorer.backend.domain.RefreshToken;
import com.fipeexplorer.backend.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @Query("select rt from RefreshToken rt join fetch rt.user where rt.token = :token")
    Optional<RefreshToken> findByToken(@Param("token") String token);

    // Query derivada (não é um @Modifying bulk delete) precisa de transação própria: sem isso,
    // chamar fora de um método @Transactional (ex.: limpeza em teste) falha com
    // "No EntityManager with actual transaction available".
    @Transactional
    void deleteByUser(User user);
}
