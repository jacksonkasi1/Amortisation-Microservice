package com.lms.amortisation;

// ** import core packages
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for Amortisation Microservice
 *
 * This microservice provides scalable amortisation calculation capabilities
 * for various loan products including home mortgage, personal loan, vehicle loan,
 * and gold loan.
 *
 * Key Features:
 * - Synchronous REST API for real-time calculations
 * - Batch processing for month-end and day-end operations
 * - Multiple amortisation methods (Reducing Balance, Flat Rate, Bullet Payment)
 * - Edge case handling (Prepayments, Payment Holidays, Rate Changes)
 * - RBI compliance with complete audit trail
 * - Redis caching for performance optimization
 * - Distributed tracing and metrics
 *
 * @author LMS Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableScheduling
public class AmortisationApplication {

    public static void main(String[] args) {
        SpringApplication.run(AmortisationApplication.class, args);
    }
}
