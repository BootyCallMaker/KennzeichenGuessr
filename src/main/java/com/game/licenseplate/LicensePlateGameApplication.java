package com.game.licenseplate;

import com.game.licenseplate.entity.CityData;
import com.game.licenseplate.repository.CityDataRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Arrays;
import java.util.List;

@SpringBootApplication
public class LicensePlateGameApplication {

    public static void main(String[] args) {
        SpringApplication.run(LicensePlateGameApplication.class, args);
    }

    @Bean
    public CommandLineRunner databaseInitializer(CityDataRepository cityRepository) {
        return args -> {
            if (cityRepository.count() == 0) {
                List<CityData> cities = Arrays.asList(
                    new CityData("Berlin", "B", 52.5200, 13.4049),
                    new CityData("München", "M", 48.1351, 11.5820),
                    new CityData("Hamburg", "HH", 53.5511, 9.9937),
                    new CityData("Köln", "K", 50.9375, 6.9603),
                    new CityData("Frankfurt am Main", "F", 50.1109, 8.6821),
                    new CityData("Stuttgart", "S", 48.7758, 9.1829),
                    new CityData("Düsseldorf", "D", 51.2277, 6.7735),
                    new CityData("Dortmund", "DO", 51.5136, 7.4653),
                    new CityData("Essen", "E", 51.4556, 7.0116),
                    new CityData("Leipzig", "L", 51.3397, 12.3731),
                    new CityData("Bremen", "HB", 53.0793, 8.8017),
                    new CityData("Dresden", "DD", 51.0504, 13.7373),
                    new CityData("Hannover", "H", 52.3759, 9.7320),
                    new CityData("Nürnberg", "N", 49.4521, 11.0767),
                    new CityData("Duisburg", "DU", 51.4344, 6.7623),
                    new CityData("Bochum", "BO", 51.4818, 7.2162),
                    new CityData("Wuppertal", "W", 51.2562, 7.1508),
                    new CityData("Bielefeld", "BI", 52.0302, 8.5325),
                    new CityData("Bonn", "BN", 50.7374, 7.0982),
                    new CityData("Münster", "MS", 51.9607, 7.6261)
                );
                cityRepository.saveAll(cities);
                System.out.println("Database seeded with " + cities.size() + " German cities with coordinates.");
            }
        };
    }
}
