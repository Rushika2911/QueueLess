package com.queueless.config;

import com.queueless.entity.Provider;
import com.queueless.entity.Service;
import com.queueless.entity.User;
import com.queueless.entity.WorkingHour;
import com.queueless.entity.enums.ProviderStatus;
import com.queueless.entity.enums.Role;
import com.queueless.repository.ProviderRepository;
import com.queueless.repository.ServiceRepository;
import com.queueless.repository.UserRepository;
import com.queueless.repository.WorkingHourRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner initDatabase(
            UserRepository userRepository,
            ProviderRepository providerRepository,
            ServiceRepository serviceRepository,
            WorkingHourRepository workingHourRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            if (userRepository.count() > 0) {
                log.info("Database already initialized with seed data.");
                return;
            }

            log.info("Populating initial development seed data...");

            // 1. Seed Admin User
            User admin = userRepository.save(new User("Admin User", "admin@queueless.com", passwordEncoder.encode("AdminPassword123"), Role.ADMIN, true));

            // 2. Seed Provider 1: Dr. Sarah Connor
            User sarahUser = userRepository.save(new User("Dr. Sarah Connor", "sarah@clinic.com", passwordEncoder.encode("DoctorPassword123"), Role.PROVIDER, true));
            Provider sarahProvider = providerRepository.save(new Provider(sarahUser, "Dental Care & Surgery", "Specialized oral care & surgery", ProviderStatus.ACTIVE));

            // 3. Seed Provider 2: Dr. James Evans
            User evansUser = userRepository.save(new User("Dr. James Evans", "evans@clinic.com", passwordEncoder.encode("DoctorPassword123"), Role.PROVIDER, true));
            Provider evansProvider = providerRepository.save(new Provider(evansUser, "Cardiology", "Heart health & ECG checkups", ProviderStatus.ACTIVE));

            // 4. Seed Customers
            userRepository.save(new User("Alice Johnson", "alice@example.com", passwordEncoder.encode("CustomerPassword123"), Role.CUSTOMER, true));
            userRepository.save(new User("Bob Smith", "bob@example.com", passwordEncoder.encode("CustomerPassword123"), Role.CUSTOMER, true));

            // 5. Seed Services
            serviceRepository.save(new Service(sarahProvider, "Teeth Cleaning & Polish", "30-min oral prophylaxis", 30, new BigDecimal("75.00"), true));
            serviceRepository.save(new Service(sarahProvider, "Tooth Extraction", "45-min minor surgical extraction", 45, new BigDecimal("150.00"), true));

            serviceRepository.save(new Service(evansProvider, "Cardiology Consultation", "30-min heart health review", 30, new BigDecimal("120.00"), true));
            serviceRepository.save(new Service(evansProvider, "ECG Analysis", "15-min electrocardiogram test", 15, new BigDecimal("60.00"), true));

            // 6. Seed Working Hours (Monday to Friday 09:00 - 17:00)
            List<DayOfWeek> workDays = List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);
            for (DayOfWeek day : workDays) {
                workingHourRepository.save(new WorkingHour(sarahProvider, day, LocalTime.of(9, 0), LocalTime.of(17, 0)));
                workingHourRepository.save(new WorkingHour(evansProvider, day, LocalTime.of(9, 0), LocalTime.of(17, 0)));
            }

            log.info("Seed data initialization complete.");
        };
    }
}
