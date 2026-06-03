package com.colorinchi.app.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;

import com.colorinchi.app.model.AnonymousOwner;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

public interface AnonymousOwnerRepository extends JpaRepository<AnonymousOwner, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    Optional<AnonymousOwner> findFirstByBootstrapTrueOrderByCreatedAtAsc();

    Optional<AnonymousOwner> findByTokenHash(String tokenHash);
}
