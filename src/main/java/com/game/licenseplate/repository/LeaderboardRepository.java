package com.game.licenseplate.repository;

import com.game.licenseplate.entity.LeaderboardEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface LeaderboardRepository extends JpaRepository<LeaderboardEntry, Long> {
    Optional<LeaderboardEntry> findByPlayerNameIgnoreCase(String playerName);
    List<LeaderboardEntry> findTop10ByOrderByScoreDesc();
}
