package com.tuckersoft.branchengine.playthrough;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaythroughRepository extends JpaRepository<Playthrough, Long> {
    boolean existsByPlayerTag(String playerTag);
    List<Playthrough> findByUserIdOrderByCreatedAtDescIdDesc(Long userId);
    List<Playthrough> findAllByOrderByCreatedAtDescIdDesc();
}
