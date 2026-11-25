package com.saferoute.sos.repository;

import com.saferoute.sos.entity.SosAlert;
import com.saferoute.sos.entity.SosStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SosAlertRepository extends JpaRepository<SosAlert, Long> {

    List<SosAlert> findByUserIdOrderByDateDesc(Long userId);

    List<SosAlert> findByStatut(SosStatus statut);

    List<SosAlert> findByStatutInOrderByDateDesc(List<SosStatus> statuts);
}
