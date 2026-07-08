package com.game.licenseplate.repository;

import com.game.licenseplate.entity.GameRound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface GameRoundRepository extends JpaRepository<GameRound, UUID> {
}
