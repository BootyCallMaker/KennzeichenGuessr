package com.game.licenseplate.repository;

import com.game.licenseplate.entity.CityData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CityDataRepository extends JpaRepository<CityData, Long> {
}
