package com.game.licenseplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.game.licenseplate.entity.CityData;
import com.game.licenseplate.repository.CityDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.List;

@SpringBootApplication
public class LicensePlateGameApplication {

    private static final Logger log = LoggerFactory.getLogger(LicensePlateGameApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(LicensePlateGameApplication.class, args);
    }

    @Bean
    public CommandLineRunner databaseInitializer(CityDataRepository cityRepository) {
        return args -> {
            if (cityRepository.count() == 0) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    InputStream inputStream = new ClassPathResource("cities.json").getInputStream();
                    List<CityData> cities = mapper.readValue(inputStream, new TypeReference<List<CityData>>() {});
                    cityRepository.saveAll(cities);
                    log.info("Database successfully seeded with {} German cities from cities.json.", cities.size());
                } catch (Exception e) {
                    log.error("Failed to seed database from cities.json", e);
                }
            }
        };
    }
}
