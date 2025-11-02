package com.lms.amortisation.model.enums;

/**
 * Amortisation calculation methods
 *
 * @author LMS Team
 * @version 1.0.0
 */
public enum AmortisationMethod {

    /**
     * Reducing Balance method (Equal EMI)
     * Interest calculated on outstanding principal
     * Most common method for home loans, personal loans
     */
    REDUCING_BALANCE("Reducing Balance", "EMI = P × r × (1+r)^n / ((1+r)^n - 1)"),

    /**
     * Flat Rate method
     * Interest calculated on original principal throughout tenure
     * Common for short-term loans
     */
    FLAT_RATE("Flat Rate", "EMI = (P + (P × r × n)) / n"),

    /**
     * Bullet Payment
     * Interest paid periodically, principal paid at maturity
     * Used for construction loans, business loans
     */
    BULLET_PAYMENT("Bullet Payment", "Interest periodic, Principal at end"),

    /**
     * Daily Reducing Balance
     * Interest calculated daily on outstanding balance
     * Common for gold loans, overdraft facilities
     */
    DAILY_REDUCING("Daily Reducing", "Interest = Outstanding × Daily Rate × Days"),

    /**
     * Step-up EMI
     * EMI increases periodically
     * Used for loans with increasing income expectations
     */
    STEP_UP("Step-up EMI", "EMI increases at predefined intervals"),

    /**
     * Step-down EMI
     * EMI decreases periodically
     * Used for loans with decreasing income expectations
     */
    STEP_DOWN("Step-down EMI", "EMI decreases at predefined intervals");

    private final String displayName;
    private final String formula;

    AmortisationMethod(String displayName, String formula) {
        this.displayName = displayName;
        this.formula = formula;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getFormula() {
        return formula;
    }
}
