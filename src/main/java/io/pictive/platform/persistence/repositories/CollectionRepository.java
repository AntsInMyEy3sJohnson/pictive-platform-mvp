package io.pictive.platform.persistence.repositories;

import io.pictive.platform.domain.collections.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface CollectionRepository extends JpaRepository<Collection, UUID> {
}
