package com.lms.amortisation.model.dto;

// ** import utils
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents a single EMI installment
 *
 * Contains breakdown of principal, interest, and balance for one installment
 *
 * @author LMS Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Installment {

    /**
     * Installment number (1 to N)
     */
    private Integer installmentNumber;

    /**
     * Due date for this installment
     */
    private LocalDate dueDate;

    /**
     * Outstanding principal at the beginning of this installment
     */
    private BigDecimal openingBalance;

    /**
     * Total EMI amount for this installment
     */
    private BigDecimal emi;

    /**
     * Principal component of this installment
     */
    private BigDecimal principal;

    /**
     * Interest component of this installment
     */
    private BigDecimal interest;

    /**
     * Outstanding principal after this installment is paid
     */
    private BigDecimal closingBalance;

    /**
     * Cumulative principal paid till this installment
     */
    private BigDecimal cumulativePrincipal;

    /**
     * Cumulative interest paid till this installment
     */
    private BigDecimal cumulativeInterest;

    /**
     * Payment status (for retrieved schedules)
     */
    private String paymentStatus;

    /**
     * Actual payment date (if paid)
     */
    private LocalDate paymentDate;

    /**
     * Actual amount paid (if different from EMI)
     */
    private BigDecimal amountPaid;
}
