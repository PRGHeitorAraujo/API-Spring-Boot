package dio.budgeting.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfig {
    /** Relógio injetável: nos testes dá para usar um Clock fixo e controlar "hoje". */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
