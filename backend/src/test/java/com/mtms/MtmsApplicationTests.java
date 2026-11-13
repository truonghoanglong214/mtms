package com.mtms;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class MtmsApplicationTests {

    @Test
    void contextLoads() {
        // Smoke test verifying Spring ApplicationContext boots successfully
    }
}
