package org.dpp.backend;

import org.dpp.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class BackendApplicationTests {

    @MockitoBean
    private UserRepository userRepository;
    
    //generierter Test, um zu überprüfen,
    // ob der Spring ApplicationContext erfolgreich geladen wird
    @Test
    void contextLoads() {
    }
}
