package com.colorinchi.app.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.colorinchi.app.model.AnonymousOwner;

public interface AnonymousOwnerRepository extends JpaRepository<AnonymousOwner, UUID> {

    Optional<AnonymousOwner> findFirstByBootstrapTrueOrderByCreatedAtAsc();

    Optional<AnonymousOwner> findByTokenHash(String tokenHash);
}
