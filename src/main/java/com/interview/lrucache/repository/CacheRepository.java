package com.interview.lrucache.repository;

import com.interview.lrucache.entity.CacheEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CacheRepository extends JpaRepository<CacheEntity, String> {
}