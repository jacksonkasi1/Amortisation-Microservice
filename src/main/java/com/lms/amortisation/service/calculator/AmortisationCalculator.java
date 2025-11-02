package com.lms.amortisation.service.calculator;

// ** import types
import com.lms.amortisation.model.dto.CalculationRequest;
import com.lms.amortisation.model.dto.EMISchedule;

/**
 * Strategy interface for different amortisation calculation methods
 *
 * Implementations should support various loan amortisation methods:
 * - Reducing Balance (Equal EMI)
 * - Flat Rate
 * - Bullet Payment
 * - Custom methods
 *
 * Each calculator must:
 * 1. Calculate EMI amount
 * 2. Generate complete installment schedule
 * 3. Split principal and interest for each installment
 * 4. Maintain calculation audit trail for compliance
 *
 * @author LMS Team
 * @version 1.0.0
 */
public interface AmortisationCalculator {

    /**
     * Calculate complete EMI schedule for a loan
     *
     * @param request Calculation request with loan parameters
     * @return Complete EMI schedule with installment breakdown
     * @throws com.lms.amortisation.exception.CalculationException if calculation fails
     */
    EMISchedule calculate(CalculationRequest request);

    /**
     * Check if this calculator supports the given amortisation method
     *
     * @param method Amortisation method to check
     * @return true if supported, false otherwise
     */
    boolean supports(String method);

    /**
     * Get the name of this calculator
     *
     * @return Calculator name (e.g., "REDUCING_BALANCE", "FLAT_RATE")
     */
    String getCalculatorName();
}
