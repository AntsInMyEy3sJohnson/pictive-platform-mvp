package io.pictive.platform.persistence;

import io.pictive.platform.domain.collection.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CollectionRepository extends JpaRepository<Collection, UUID> {
}