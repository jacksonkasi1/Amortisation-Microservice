package com.lms.amortisation.controller;

// ** import types
import com.lms.amortisation.model.dto.CalculationRequest;
import com.lms.amortisation.model.dto.EMISchedule;
import com.lms.amortisation.service.AmortisationService;

// ** import core packages
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// ** import validation
import jakarta.validation.Valid;

// ** import utils
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST Controller for amortisation calculations
 *
 * Provides endpoints for:
 * - Real-time EMI calculation
 * - Schedule retrieval
 * - Recalculation with edge cases
 *
 * @author LMS Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/amortisation")
@RequiredArgsConstructor
@Tag(name = "Amortisation", description = "Amortisation calculation APIs")
@SecurityRequirement(name = "OAuth2")
public class AmortisationController {

    private final AmortisationService amortisationService;

    /**
     * Calculate EMI schedule for a loan
     *
     * @param request Calculation request with loan parameters
     * @return Complete EMI schedule with installment breakdown
     */
    @PostMapping("/calculate")
    @PreAuthorize("hasAuthority('SCOPE_amortisation:calculate')")
    @Timed(value = "amortisation.calculate", description = "Time taken to calculate EMI")
    @Operation(
        summary = "Calculate EMI Schedule",
        description = "Calculate complete EMI schedule with principal/interest breakdown for each installment"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Calculation successful"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<EMISchedule> calculateEMI(
        @Valid @RequestBody CalculationRequest request
    ) {
        log.info("Received calculation request for loanId: {}, productType: {}, method: {}",
            request.getLoanId(), request.getProductType(), request.getAmortisationMethod());

        EMISchedule schedule = amortisationService.calculate(request);

        log.info("Calculation completed for loanId: {}. EMI: {}, Installments: {}",
            request.getLoanId(), schedule.getEmi(), schedule.getInstallmentCount());

        return ResponseEntity.ok(schedule);
    }

    /**
     * Get existing loan schedule
     *
     * @param loanId Loan identifier
     * @return EMI schedule if exists (may be from cache)
     */
    @GetMapping("/schedule/{loanId}")
    @PreAuthorize("hasAuthority('SCOPE_amortisation:calculate')")
    @Timed(value = "amortisation.get.schedule", description = "Time taken to retrieve schedule")
    @Operation(
        summary = "Get Loan Schedule",
        description = "Retrieve existing EMI schedule for a loan (cached result if available)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Schedule retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Schedule not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<EMISchedule> getSchedule(
        @PathVariable String loanId
    ) {
        log.info("Retrieving schedule for loanId: {}", loanId);

        EMISchedule schedule = amortisationService.getSchedule(loanId);

        if (schedule == null) {
            log.warn("Schedule not found for loanId: {}", loanId);
            return ResponseEntity.notFound().build();
        }

        log.info("Schedule retrieved for loanId: {}. Cached: {}", loanId, schedule.isCached());
        return ResponseEntity.ok(schedule);
    }

    /**
     * Health check endpoint
     *
     * @return OK if service is healthy
     */
    @GetMapping("/health")
    @Operation(summary = "Health Check", description = "Check if amortisation service is healthy")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Amortisation Service is UP");
    }
}
