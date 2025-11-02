package com.lms.amortisation.model.dto;

// ** import utils
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Response DTO containing complete EMI schedule
 *
 * @author LMS Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EMISchedule {

    /**
     * Request ID for tracing
     */
    private String requestId;

    /**
     * Loan ID
     */
    private String loanId;

    /**
     * Timestamp when calculation was performed
     */
    @Builder.Default
    private Instant calculatedAt = Instant.now();

    /**
     * Monthly EMI amount
     */
    private BigDecimal emi;

    /**
     * Total interest to be paid over loan tenure
     */
    private BigDecimal totalInterest;

    /**
     * Total amount to be paid (principal + interest)
     */
    private BigDecimal totalPayment;

    /**
     * Complete installment schedule
     */
    private List<Installment> schedule;

    /**
     * Audit trail for compliance
     * Contains calculation method, formula, and parameters
     */
    private String auditTrail;

    /**
     * Calculation method used
     */
    private String calculationMethod;

    /**
     * Cached indicator
     */
    @Builder.Default
    private boolean cached = false;

    /**
     * Get number of installments
     *
     * @return Total number of installments
     */
    public int getInstallmentCount() {
        return schedule != null ? schedule.size() : 0;
    }
}
