package com.saferoute.sos.service;

import com.saferoute.sos.dto.CreateEmergencyContactRequest;
import com.saferoute.sos.entity.EmergencyContact;
import com.saferoute.sos.exception.ResourceNotFoundException;
import com.saferoute.sos.repository.EmergencyContactRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class EmergencyContactService {

    private final EmergencyContactRepository emergencyContactRepository;

    public EmergencyContactService(EmergencyContactRepository emergencyContactRepository) {
        this.emergencyContactRepository = emergencyContactRepository;
    }

    @Transactional(readOnly = true)
    public List<EmergencyContact> getContactsByUserId(Long userId) {
        Long targetUserId = (userId != null) ? userId : 1L;
        return emergencyContactRepository.findByUserId(targetUserId);
    }

    public EmergencyContact addContact(CreateEmergencyContactRequest request, Long authenticatedUserId) {
        Long targetUserId = (authenticatedUserId != null) ? authenticatedUserId :
                (request.getUserId() != null ? request.getUserId() : 1L);

        EmergencyContact contact = new EmergencyContact(
                targetUserId,
                request.getNom().trim(),
                request.getTelephone().trim(),
                request.getRelation() != null ? request.getRelation().trim() : null
        );
        return emergencyContactRepository.save(contact);
    }

    public void deleteContact(Long id) {
        if (!emergencyContactRepository.existsById(id)) {
            throw new ResourceNotFoundException("Contact d'urgence introuvable avec l'identifiant : " + id);
        }
        emergencyContactRepository.deleteById(id);
    }
}
