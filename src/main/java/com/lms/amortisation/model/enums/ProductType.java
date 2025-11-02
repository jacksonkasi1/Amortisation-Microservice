package com.lms.amortisation.model.enums;

/**
 * Loan product types supported by the LMS
 *
 * @author LMS Team
 * @version 1.0.0
 */
public enum ProductType {

    /**
     * Home mortgage loan
     */
    HOME_LOAN("Home Loan", "Residential mortgage loan"),

    /**
     * Personal loan
     */
    PERSONAL_LOAN("Personal Loan", "Unsecured personal loan"),

    /**
     * Vehicle loan
     */
    VEHICLE_LOAN("Vehicle Loan", "Auto/vehicle financing"),

    /**
     * Gold loan
     */
    GOLD_LOAN("Gold Loan", "Loan against gold"),

    /**
     * Business loan
     */
    BUSINESS_LOAN("Business Loan", "SME/Business financing"),

    /**
     * Education loan
     */
    EDUCATION_LOAN("Education Loan", "Student loan"),

    /**
     * Loan against property
     */
    LOAN_AGAINST_PROPERTY("Loan Against Property", "Loan secured by property");

    private final String displayName;
    private final String description;

    ProductType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
