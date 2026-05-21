package com.interview.lrucache.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "cache_entries")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class CacheEntity {

    @Id
    private String cacheKey;
    private String cacheValue;
    private Instant lastAccessed;
}