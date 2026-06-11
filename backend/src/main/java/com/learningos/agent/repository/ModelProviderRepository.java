package com.learningos.agent.repository;

import com.learningos.agent.domain.ModelProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ModelProviderRepository extends JpaRepository<ModelProvider, String> {

    Optional<ModelProvider> findByProviderCode(String providerCode);

    Optional<ModelProvider> findFirstByEnabledTrueAndDefaultProviderTrueOrderByUpdatedAtDesc();

    List<ModelProvider> findAllByOrderByDisplayNameAsc();

    @Modifying
    @Query("update ModelProvider provider set provider.defaultProvider = false where provider.defaultProvider = true")
    int clearDefaultProviders();
}
