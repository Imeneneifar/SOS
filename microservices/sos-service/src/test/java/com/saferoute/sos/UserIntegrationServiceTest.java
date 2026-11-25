package com.saferoute.sos;

import com.saferoute.sos.client.UserServiceClient;
import com.saferoute.sos.client.dto.UserEmergencyContactDto;
import com.saferoute.sos.entity.EmergencyContact;
import com.saferoute.sos.repository.EmergencyContactRepository;
import com.saferoute.sos.service.UserIntegrationService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class UserIntegrationServiceTest {

    @Test
    void whenFeignEnabledAndUserServiceResponds_returnsRemoteContacts() {
        UserServiceClient feignClient = Mockito.mock(UserServiceClient.class);
        EmergencyContactRepository localRepo = Mockito.mock(EmergencyContactRepository.class);

        when(feignClient.getEmergencyContactsByUserId(1L))
                .thenReturn(List.of(new UserEmergencyContactDto(10L, 1L, "Mère", "+216 20 000 111", "Mère")));

        UserIntegrationService service = new UserIntegrationService(feignClient, localRepo, true);

        List<UserEmergencyContactDto> result = service.resolveEmergencyContacts(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNom()).isEqualTo("Mère");
    }

    @Test
    void whenFeignFails_fallsBackGracefullyToLocalRepository() {
        UserServiceClient feignClient = Mockito.mock(UserServiceClient.class);
        EmergencyContactRepository localRepo = Mockito.mock(EmergencyContactRepository.class);

        when(feignClient.getEmergencyContactsByUserId(1L))
                .thenThrow(new RuntimeException("user-service unreachable"));
        EmergencyContact local = new EmergencyContact(1L, "Contact Secours Local", "+216 98 111 222", "Père");
        local.setId(5L);
        when(localRepo.findByUserId(1L)).thenReturn(List.of(local));

        UserIntegrationService service = new UserIntegrationService(feignClient, localRepo, true);

        List<UserEmergencyContactDto> result = service.resolveEmergencyContacts(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNom()).isEqualTo("Contact Secours Local");
    }
}
