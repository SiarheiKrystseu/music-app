package com.krystseu.microservices.resourceservice.repository;

import com.krystseu.microservices.resourceservice.model.Resource;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceRepository extends JpaRepository<Resource, Long> {
}
