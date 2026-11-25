package com.saferoute.sos.config;

import com.saferoute.sos.entity.EmergencyContact;
import com.saferoute.sos.entity.SosAlert;
import com.saferoute.sos.entity.SosStatus;
import com.saferoute.sos.repository.EmergencyContactRepository;
import com.saferoute.sos.repository.SosAlertRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner initDatabase(SosAlertRepository alertRepository, EmergencyContactRepository contactRepository) {
        return args -> {
            if (alertRepository.count() == 0) {
                log.info("📢 Initialisation des données de test SOS...");

                SosAlert alert1 = new SosAlert();
                alert1.setUserId(1L);
                alert1.setLatitude(36.8065);
                alert1.setLongitude(10.1815);
                alert1.setMessage("Alerte agression suspectée près de la station de métro");
                alert1.setStatut(SosStatus.ACTIVE);
                alert1.setDate(LocalDateTime.now().minusMinutes(15));
                alertRepository.save(alert1);

                SosAlert alert2 = new SosAlert();
                alert2.setUserId(2L);
                alert2.setLatitude(36.8120);
                alert2.setLongitude(10.1700);
                alert2.setMessage("Accident de la route mineur, besoin d'assistance");
                alert2.setStatut(SosStatus.EN_COURS);
                alert2.setDate(LocalDateTime.now().minusMinutes(45));
                alertRepository.save(alert2);

                SosAlert alert3 = new SosAlert();
                alert3.setUserId(1L);
                alert3.setLatitude(36.7990);
                alert3.setLongitude(10.1650);
                alert3.setMessage("Problème résolu, les secours sont intervenus");
                alert3.setStatut(SosStatus.RESOLUE);
                alert3.setDate(LocalDateTime.now().minusHours(2));
                alertRepository.save(alert3);

                log.info("✅ 3 alertes SOS de démonstration créées avec succès !");
            }

            if (contactRepository.count() == 0) {
                contactRepository.save(new EmergencyContact(1L, "Ahmed Trabelsi", "+216 98 123 456", "Père"));
                contactRepository.save(new EmergencyContact(1L, "Sarra Ben Ali", "+216 22 987 654", "Soeur"));
                log.info("✅ 2 contacts d'urgence de test créés avec succès !");
            }
        };
    }
}
