package com.game.licenseplate.service;

import com.game.licenseplate.entity.LeaderboardEntry;
import com.game.licenseplate.repository.LeaderboardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class LeaderboardService {

    private final LeaderboardRepository leaderboardRepository;

    public LeaderboardService(LeaderboardRepository leaderboardRepository) {
        this.leaderboardRepository = leaderboardRepository;
    }

    @Transactional
    public LeaderboardEntry submitScore(String playerName, int score) {
        String cleanName = playerName.trim();
        Optional<LeaderboardEntry> entryOpt = leaderboardRepository.findByPlayerNameIgnoreCase(cleanName);
        
        LeaderboardEntry entry;
        if (entryOpt.isPresent()) {
            entry = entryOpt.get();
            // Update only if new score is higher
            if (score > entry.getScore()) {
                entry.setScore(score);
                entry.setUpdatedAt(LocalDateTime.now());
                entry = leaderboardRepository.save(entry);
            }
        } else {
            entry = new LeaderboardEntry(cleanName, score);
            entry = leaderboardRepository.save(entry);
        }
        return entry;
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntry> getTop10() {
        return leaderboardRepository.findTop10ByOrderByScoreDesc();
    }
}
