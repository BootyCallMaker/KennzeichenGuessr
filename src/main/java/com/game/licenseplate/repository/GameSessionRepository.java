package com.game.licenseplate.repository;

import com.game.licenseplate.entity.GameSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface GameSessionRepository extends JpaRepository<GameSession, UUID> {
}
