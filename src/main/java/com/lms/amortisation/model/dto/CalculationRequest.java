package com.lms.amortisation.model.dto;

// ** import types
import com.lms.amortisation.model.enums.AmortisationMethod;
import com.lms.amortisation.model.enums.ProductType;

// ** import validation
import jakarta.validation.constraints.*;

// ** import utils
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Request DTO for amortisation calculation
 *
 * Contains all necessary parameters for calculating loan EMI schedule
 *
 * @author LMS Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalculationRequest {

    /**
     * Unique loan identifier
     */
    @NotBlank(message = "Loan ID is required")
    private String loanId;

    /**
     * Loan principal amount
     */
    @NotNull(message = "Principal amount is required")
    @DecimalMin(value = "10000.00", message = "Principal must be at least 10,000")
    @DecimalMax(value = "100000000.00", message = "Principal cannot exceed 10 crores")
    private BigDecimal principal;

    /**
     * Annual interest rate (e.g., 8.5 for 8.5%)
     */
    @NotNull(message = "Interest rate is required")
    @DecimalMin(value = "0.0", message = "Interest rate cannot be negative")
    @DecimalMax(value = "50.0", message = "Interest rate cannot exceed 50%")
    private BigDecimal interestRate;

    /**
     * Loan tenure in months
     */
    @NotNull(message = "Tenure is required")
    @Min(value = 1, message = "Tenure must be at least 1 month")
    @Max(value = 360, message = "Tenure cannot exceed 360 months")
    private Integer tenure;

    /**
     * Product type (e.g., HOME_LOAN, PERSONAL_LOAN)
     */
    @NotNull(message = "Product type is required")
    private ProductType productType;

    /**
     * Amortisation method (e.g., REDUCING_BALANCE, FLAT_RATE)
     */
    @NotNull(message = "Amortisation method is required")
    private AmortisationMethod amortisationMethod;

    /**
     * Loan start date
     */
    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    /**
     * Payment frequency (default: MONTHLY)
     */
    @Builder.Default
    private String frequency = "MONTHLY";

    /**
     * Additional options for calculation
     * Can include:
     * - includePrepayments: boolean
     * - includeHolidays: boolean
     * - calculateInsurance: boolean
     */
    private Map<String, Object> options;

    /**
     * User ID who requested the calculation (for audit)
     */
    private String requestedBy;

    /**
     * Check if a specific option is enabled
     *
     * @param optionKey Option key
     * @return true if option is enabled, false otherwise
     */
    public boolean isOptionEnabled(String optionKey) {
        if (options == null) {
            return false;
        }
        Object value = options.get(optionKey);
        return value instanceof Boolean && (Boolean) value;
    }
}
