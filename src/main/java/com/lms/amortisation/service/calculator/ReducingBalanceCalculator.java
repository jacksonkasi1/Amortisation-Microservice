package com.lms.amortisation.service.calculator;

// ** import types
import com.lms.amortisation.model.dto.CalculationRequest;
import com.lms.amortisation.model.dto.EMISchedule;
import com.lms.amortisation.model.dto.Installment;
import com.lms.amortisation.model.enums.AmortisationMethod;
import com.lms.amortisation.exception.CalculationException;

// ** import core packages
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

// ** import utils
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Reducing Balance Amortisation Calculator
 *
 * Calculates EMI using the reducing balance method where:
 * - EMI remains constant throughout the loan tenure
 * - Interest is calculated on the outstanding principal
 * - Principal component increases while interest component decreases over time
 *
 * Formula: EMI = P × r × (1+r)^n / ((1+r)^n - 1)
 * Where:
 *   P = Principal loan amount
 *   r = Monthly interest rate (annual rate / 12 / 100)
 *   n = Number of monthly installments
 *
 * @author LMS Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class ReducingBalanceCalculator implements AmortisationCalculator {

    private static final int DECIMAL_PRECISION = 15;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    @Override
    public EMISchedule calculate(CalculationRequest request) {
        log.debug("Starting reducing balance calculation for loanId: {}", request.getLoanId());

        try {
            // Validate input
            validateRequest(request);

            // Extract parameters
            BigDecimal principal = request.getPrincipal();
            BigDecimal annualRate = request.getInterestRate();
            int tenure = request.getTenure();
            LocalDate startDate = request.getStartDate();

            // Calculate monthly interest rate
            BigDecimal monthlyRate = calculateMonthlyRate(annualRate);

            // Calculate EMI
            BigDecimal emi = calculateEMI(principal, monthlyRate, tenure);

            // Generate installment schedule
            List<Installment> schedule = generateSchedule(
                principal, emi, monthlyRate, tenure, startDate
            );

            // Calculate totals
            BigDecimal totalInterest = calculateTotalInterest(schedule);
            BigDecimal totalPayment = principal.add(totalInterest);

            // Build audit trail
            String auditTrail = buildAuditTrail(principal, annualRate, monthlyRate, tenure, emi);

            log.info("Calculation completed for loanId: {}. EMI: {}, Total Interest: {}",
                request.getLoanId(), emi, totalInterest);

            return EMISchedule.builder()
                .loanId(request.getLoanId())
                .emi(emi.setScale(2, ROUNDING_MODE))
                .totalInterest(totalInterest.setScale(2, ROUNDING_MODE))
                .totalPayment(totalPayment.setScale(2, ROUNDING_MODE))
                .schedule(schedule)
                .auditTrail(auditTrail)
                .calculationMethod(AmortisationMethod.REDUCING_BALANCE.name())
                .build();

        } catch (Exception e) {
            log.error("Calculation failed for loanId: {}", request.getLoanId(), e);
            throw new CalculationException("Failed to calculate amortisation schedule", e);
        }
    }

    /**
     * Calculate monthly interest rate from annual rate
     *
     * @param annualRate Annual interest rate (e.g., 8.5 for 8.5%)
     * @return Monthly interest rate as decimal (e.g., 0.00708333 for 8.5% annual)
     */
    private BigDecimal calculateMonthlyRate(BigDecimal annualRate) {
        return annualRate
            .divide(BigDecimal.valueOf(12), DECIMAL_PRECISION, ROUNDING_MODE)
            .divide(BigDecimal.valueOf(100), DECIMAL_PRECISION, ROUNDING_MODE);
    }

    /**
     * Calculate EMI using reducing balance formula
     *
     * Formula: EMI = P × r × (1+r)^n / ((1+r)^n - 1)
     *
     * @param principal Loan principal amount
     * @param monthlyRate Monthly interest rate (as decimal)
     * @param tenure Number of months
     * @return Monthly EMI amount
     */
    private BigDecimal calculateEMI(BigDecimal principal, BigDecimal monthlyRate, int tenure) {
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            // Zero interest case
            return principal.divide(BigDecimal.valueOf(tenure), 2, ROUNDING_MODE);
        }

        // Calculate (1 + r)^n
        BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
        BigDecimal onePlusRatePowerN = onePlusRate.pow(tenure);

        // Calculate numerator: P × r × (1+r)^n
        BigDecimal numerator = principal
            .multiply(monthlyRate)
            .multiply(onePlusRatePowerN);

        // Calculate denominator: (1+r)^n - 1
        BigDecimal denominator = onePlusRatePowerN.subtract(BigDecimal.ONE);

        // Calculate EMI
        return numerator.divide(denominator, 2, ROUNDING_MODE);
    }

    /**
     * Generate complete installment schedule
     *
     * @param principal Loan principal
     * @param emi Monthly EMI amount
     * @param monthlyRate Monthly interest rate
     * @param tenure Number of months
     * @param startDate Loan start date
     * @return List of installments with principal/interest split
     */
    private List<Installment> generateSchedule(
        BigDecimal principal,
        BigDecimal emi,
        BigDecimal monthlyRate,
        int tenure,
        LocalDate startDate
    ) {
        List<Installment> schedule = new ArrayList<>(tenure);
        BigDecimal outstandingBalance = principal;
        BigDecimal cumulativePrincipal = BigDecimal.ZERO;
        BigDecimal cumulativeInterest = BigDecimal.ZERO;

        for (int i = 1; i <= tenure; i++) {
            // Calculate interest for this month
            BigDecimal interest = outstandingBalance
                .multiply(monthlyRate)
                .setScale(2, ROUNDING_MODE);

            // Calculate principal component
            BigDecimal principalComponent = emi.subtract(interest);

            // Adjust last installment for rounding differences
            if (i == tenure) {
                principalComponent = outstandingBalance;
                interest = emi.subtract(principalComponent);
            }

            // Update cumulative amounts
            cumulativePrincipal = cumulativePrincipal.add(principalComponent);
            cumulativeInterest = cumulativeInterest.add(interest);

            // Calculate closing balance
            BigDecimal closingBalance = outstandingBalance.subtract(principalComponent);

            // Calculate due date (add i months to start date)
            LocalDate dueDate = startDate.plusMonths(i);

            // Create installment
            Installment installment = Installment.builder()
                .installmentNumber(i)
                .dueDate(dueDate)
                .openingBalance(outstandingBalance.setScale(2, ROUNDING_MODE))
                .emi(emi)
                .principal(principalComponent.setScale(2, ROUNDING_MODE))
                .interest(interest)
                .closingBalance(closingBalance.setScale(2, ROUNDING_MODE))
                .cumulativePrincipal(cumulativePrincipal.setScale(2, ROUNDING_MODE))
                .cumulativeInterest(cumulativeInterest.setScale(2, ROUNDING_MODE))
                .build();

            schedule.add(installment);

            // Update outstanding balance for next iteration
            outstandingBalance = closingBalance;
        }

        return schedule;
    }

    /**
     * Calculate total interest paid over loan tenure
     *
     * @param schedule List of installments
     * @return Total interest amount
     */
    private BigDecimal calculateTotalInterest(List<Installment> schedule) {
        return schedule.stream()
            .map(Installment::getInterest)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Build audit trail for compliance
     *
     * @param principal Loan principal
     * @param annualRate Annual interest rate
     * @param monthlyRate Monthly interest rate
     * @param tenure Tenure in months
     * @param emi Calculated EMI
     * @return Audit trail string
     */
    private String buildAuditTrail(
        BigDecimal principal,
        BigDecimal annualRate,
        BigDecimal monthlyRate,
        int tenure,
        BigDecimal emi
    ) {
        return String.format(
            "Amortisation Method: REDUCING_BALANCE | " +
            "Formula: EMI = P × r × (1+r)^n / ((1+r)^n - 1) | " +
            "Parameters: P=%s, Annual Rate=%s%%, Monthly Rate=%s, n=%d | " +
            "Calculated EMI: %s | " +
            "Regulatory Version: RBI-2024-v1",
            principal, annualRate, monthlyRate, tenure, emi
        );
    }

    /**
     * Validate calculation request
     *
     * @param request Calculation request
     * @throws CalculationException if validation fails
     */
    private void validateRequest(CalculationRequest request) {
        if (request.getPrincipal() == null || request.getPrincipal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new CalculationException("Principal must be greater than zero");
        }

        if (request.getInterestRate() == null || request.getInterestRate().compareTo(BigDecimal.ZERO) < 0) {
            throw new CalculationException("Interest rate must be non-negative");
        }

        if (request.getTenure() <= 0) {
            throw new CalculationException("Tenure must be greater than zero");
        }

        if (request.getTenure() > 360) {
            throw new CalculationException("Tenure cannot exceed 360 months");
        }

        if (request.getStartDate() == null) {
            throw new CalculationException("Start date is required");
        }
    }

    @Override
    public boolean supports(String method) {
        return AmortisationMethod.REDUCING_BALANCE.name().equalsIgnoreCase(method);
    }

    @Override
    public String getCalculatorName() {
        return AmortisationMethod.REDUCING_BALANCE.name();
    }
}
