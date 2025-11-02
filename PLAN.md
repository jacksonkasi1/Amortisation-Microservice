# Amortisation Microservice - Detailed Implementation Plan

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Project Context](#project-context)
3. [Architecture Overview](#architecture-overview)
4. [Phase 1: Foundation (Months 1-3)](#phase-1-foundation-months-1-3)
5. [Phase 2: Optimization (Months 4-6)](#phase-2-optimization-months-4-6)
6. [Phase 3: Event-Driven (Months 7-9)](#phase-3-event-driven-months-7-9)
7. [Team Structure & Responsibilities](#team-structure--responsibilities)
8. [Risk Management](#risk-management)
9. [Quality Assurance](#quality-assurance)
10. [Success Metrics](#success-metrics)
11. [Dependencies & Prerequisites](#dependencies--prerequisites)
12. [Budget & Resources](#budget--resources)

---

## Executive Summary

### Project Goal
Extract the amortisation engine from a legacy .NET monolith LMS system and deploy it as a scalable, cloud-native microservice on AWS EKS.

### Business Drivers
- **Scalability**: Handle 300 TPS with <5s latency
- **Reliability**: Achieve 99.99% availability
- **Compliance**: Maintain RBI audit requirements
- **Performance**: Process 5M active loans efficiently
- **Modernization**: Enable future feature development

### Timeline
- **Total Duration**: 9 months
- **Phase 1**: Months 1-3 (Foundation)
- **Phase 2**: Months 4-6 (Optimization)
- **Phase 3**: Months 7-9 (Event-Driven)

### Investment
- **Team Size**: 11 FTE
- **AWS Cost**: ~$1,600/month
- **Total Budget**: ~$50,000

---

## Project Context

### Current State

#### Monolith Architecture
```
┌─────────────────────────────────────────────────────────┐
│           .NET Monolith (On-Premise)                    │
│                                                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐ │
│  │ Loan         │  │ Amortisation │  │ Payment      │ │
│  │ Origination  │──│ Engine       │──│ Processing   │ │
│  └──────────────┘  └──────────────┘  └──────────────┘ │
│                           │                            │
│                           ▼                            │
│                  ┌─────────────────┐                   │
│                  │  Oracle DB      │                   │
│                  │  (On-Premise)   │                   │
│                  └─────────────────┘                   │
└─────────────────────────────────────────────────────────┘
```

#### Current System Characteristics
- **Scale**: 5M active loans, 50K new loans/month
- **Products**: Home mortgage, personal loan, vehicle loan, gold loan
- **Calculation Methods**: Reducing balance, flat rate, bullet payment
- **Edge Cases**: Prepayments, payment holidays, rate changes
- **Performance Issues**:
  - Slow during peak hours
  - Month-end batch processing takes >8 hours
  - Limited horizontal scalability

#### Technical Debt
- Amortisation logic embedded in .NET code and Oracle stored procedures
- No documentation of calculation formulas
- Tight coupling with other modules
- Limited test coverage
- No audit trail for calculations

### Target State

#### Microservice Architecture
```
┌─────────────────┐         ┌──────────────────┐
│  .NET Monolith  │         │  API Gateway     │
│  (On-Premise)   │────────▶│  (Kong/AWS)      │
└─────────────────┘  OAuth2 └────────┬─────────┘
                                     │
                                     ▼
                            ┌─────────────────┐
                            │  Amortisation   │
                            │  Microservice   │
                            │  (Java/EKS)     │
                            └────────┬────────┘
                                     │
                    ┌────────────────┼────────────────┐
                    │                │                │
            ┌───────▼─────────┐  ┌──▼──────┐  ┌─────▼──────┐
            │  Redis Cluster  │  │ Oracle  │  │ Monitoring │
            │  (Cache)        │  │ DB      │  │ Stack      │
            └─────────────────┘  └─────────┘  └────────────┘
```

#### Target Characteristics
- **Scalability**: Auto-scale 3-20 pods based on load
- **Performance**: <5s P95 latency, 300 TPS throughput
- **Availability**: 99.99% uptime
- **Compliance**: Complete audit trail, RBI compliant
- **Batch**: Process 500K loans in <6 hours
- **Monitoring**: Real-time metrics, distributed tracing

---

## Architecture Overview

### Key Design Decisions

#### 1. Technology Stack
| Layer | Technology | Rationale |
|-------|-----------|-----------|
| Language | Java 21 | LTS support, performance, ecosystem |
| Framework | Spring Boot 3.2 | Production-ready, comprehensive features |
| Database | Oracle (Phase 1) → PostgreSQL (Phase 3) | Minimize initial changes, migrate later |
| Cache | Redis 7.x | High performance, proven at scale |
| Container | Docker + EKS | Industry standard, AWS native |
| Security | OAuth2 + Spring Security | Enterprise-grade security |
| Batch | Spring Batch | Robust framework for large-scale processing |
| Monitoring | Prometheus + Grafana + CloudWatch + DataDog | Comprehensive observability |

#### 2. Integration Patterns

**Phase 1: Synchronous Integration**
```
.NET Monolith → REST API → Microservice → Oracle DB
             ←──────────←─────────────←
```

**Phase 2: Hybrid Integration**
```
.NET Monolith → REST API → Microservice → Oracle DB
             ←──────────←

Scheduler → Batch API → Microservice → Oracle DB
          ←───Callback←
```

**Phase 3: Event-Driven**
```
Oracle DB → CDC (Debezium) → Kafka → Microservice → PostgreSQL
                                    → Event Bus → Consumers
```

#### 3. Data Strategy

**Phase 1: Shared Database**
- Read-only access to Oracle
- No data migration
- Minimal disruption

**Phase 2: Dual Write (Optional)**
- Continue reading from Oracle
- Write audit logs to separate DB

**Phase 3: Database Migration**
- CDC pipeline for real-time replication
- PostgreSQL as primary database
- Oracle as read-only fallback

#### 4. Security Architecture

```
┌─────────────────────────────────────────────────────┐
│                  Security Layers                     │
├─────────────────────────────────────────────────────┤
│ 1. Network Security                                 │
│    - VPN/Direct Connect (On-prem ↔ AWS)            │
│    - Security Groups (least privilege)              │
│    - WAF (OWASP top 10 protection)                 │
├─────────────────────────────────────────────────────┤
│ 2. API Security                                     │
│    - OAuth2 authentication                          │
│    - JWT token validation                           │
│    - Scope-based authorization                      │
│    - Rate limiting (300 TPS)                        │
├─────────────────────────────────────────────────────┤
│ 3. Data Security                                    │
│    - TLS 1.3 in transit                            │
│    - Encryption at rest (EBS volumes)              │
│    - Secrets Manager (credentials)                  │
│    - Database encryption (Oracle TDE)              │
├─────────────────────────────────────────────────────┤
│ 4. Compliance                                       │
│    - Audit logging (7-year retention)              │
│    - RBI compliance checks                          │
│    - Calculation verification                       │
│    - User activity tracking                         │
└─────────────────────────────────────────────────────┘
```

---

## Phase 1: Foundation (Months 1-3)

### Objective
Build and deploy a production-ready microservice with 10% traffic migration.

### Success Criteria
- ✅ 100% calculation parity with legacy system
- ✅ P95 latency < 5 seconds
- ✅ 99.99% availability
- ✅ Complete audit trail
- ✅ 10% production traffic
- ✅ Zero high-severity incidents

---

### Sprint 1: Project Setup & Reverse Engineering
**Duration**: Weeks 1-2
**Team**: Full team (11 FTE)

#### Week 1: Environment & Repository Setup

##### Day 1-2: Infrastructure Setup
**Owner**: DevOps Engineer + Tech Lead

**Tasks**:
1. **Create Project Structure**
   ```bash
   # Create Git repository
   mkdir amortisation-microservice
   cd amortisation-microservice
   git init

   # Create directory structure
   mkdir -p src/main/java/com/lms/amortisation/{config,controller,service,repository,model,exception,util}
   mkdir -p src/main/resources/{db/changelog}
   mkdir -p src/test/java/com/lms/amortisation/{service,integration,performance}
   mkdir -p infrastructure/{kubernetes,helm,terraform,docker}
   mkdir -p monitoring/{grafana,prometheus,cloudwatch}
   mkdir -p docs
   mkdir -p scripts/{reverse-engineer,migration,deployment}
   ```

2. **Initialize Maven Project**
   - Copy pom.xml template
   - Configure parent POM
   - Add all dependencies
   - Configure Jib plugin
   - Configure JaCoCo plugin

3. **Set Up Version Control**
   ```bash
   # Create .gitignore
   cat > .gitignore <<EOF
   target/
   *.class
   *.jar
   *.war
   .idea/
   *.iml
   .env
   application-local.yml
   EOF

   # Create branches
   git checkout -b develop
   git checkout -b feature/project-setup
   ```

4. **Configure IDE**
   - IntelliJ IDEA setup
   - Java 21 SDK configuration
   - Lombok plugin installation
   - Code style configuration (import organization)

**Deliverables**:
- ✅ Git repository with branch strategy
- ✅ Maven project structure
- ✅ IDE configuration

**Acceptance Criteria**:
- Maven build succeeds: `mvn clean install`
- All team members can clone and build

---

##### Day 3-5: AWS Infrastructure Setup
**Owner**: DevOps Engineer

**Tasks**:
1. **AWS Account Configuration**
   - Create/configure AWS account
   - Set up IAM roles and policies
   - Configure billing alerts
   - Set up AWS CLI and credentials

2. **Networking Setup**
   ```hcl
   # VPC Configuration
   - VPC CIDR: 10.0.0.0/16
   - Public Subnets: 10.0.1.0/24, 10.0.2.0/24, 10.0.3.0/24
   - Private Subnets: 10.0.11.0/24, 10.0.12.0/24, 10.0.13.0/24
   - NAT Gateways: 3 (one per AZ)
   - Internet Gateway: 1
   ```

3. **Direct Connect/VPN Setup**
   - Work with network team for Direct Connect
   - Alternative: Set up Site-to-Site VPN
   - Configure routing tables
   - Test connectivity to on-premise Oracle

4. **Security Groups**
   ```yaml
   # EKS Node Security Group
   Ingress:
     - Port 443 (from ALB)
     - Port 8080 (from ALB)

   # RDS Proxy Security Group
   Ingress:
     - Port 1521 (from EKS nodes)

   # Redis Security Group
   Ingress:
     - Port 6379 (from EKS nodes)
   ```

5. **Test Oracle Connectivity**
   ```bash
   # From AWS EC2 instance
   sqlplus lms_user/password@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST=on-prem-oracle)(PORT=1521))(CONNECT_DATA=(SERVICE_NAME=ORCL)))
   ```

**Deliverables**:
- ✅ VPC with subnets and routing
- ✅ Direct Connect or VPN configured
- ✅ Security groups created
- ✅ Oracle connectivity verified (<10ms latency)

**Acceptance Criteria**:
- Ping to on-premise Oracle: <10ms
- SQL query from AWS to Oracle: <200ms
- Security groups follow least privilege

---

#### Week 2: Reverse Engineering Legacy System

##### Day 6-8: Oracle Stored Procedures Analysis
**Owner**: Senior Java Developer #1 + DBA

**Tasks**:
1. **Identify All Amortisation Procedures**
   ```sql
   -- Query to find all stored procedures
   SELECT object_name, object_type, status
   FROM all_objects
   WHERE object_type IN ('PROCEDURE', 'FUNCTION', 'PACKAGE')
   AND object_name LIKE '%AMORT%'
   OR object_name LIKE '%EMI%'
   OR object_name LIKE '%SCHEDULE%';
   ```

2. **Extract PL/SQL Code**
   ```sql
   -- Extract procedure source
   SELECT text
   FROM all_source
   WHERE name = 'CALCULATE_EMI'
   ORDER BY line;
   ```

   Document procedures for:
   - Home Loan EMI calculation
   - Personal Loan EMI calculation
   - Vehicle Loan EMI calculation
   - Gold Loan EMI calculation
   - Prepayment processing
   - Payment holiday handling
   - Interest rate change processing

3. **Analyze Database Schema**
   ```sql
   -- Key tables to analyze
   - LOANS (loan master)
   - PRODUCT_CONFIG (product configurations)
   - INTEREST_RATE_MASTER (interest rates)
   - LOAN_EVENTS (prepayments, holidays)
   - EMI_SCHEDULE (calculated schedules)
   - PAYMENT_TRANSACTIONS (actual payments)
   ```

4. **Document Table Relationships**
   ```
   LOANS ──┬── PRODUCT_CONFIG
           ├── INTEREST_RATE_MASTER
           ├── LOAN_EVENTS
           ├── EMI_SCHEDULE
           └── PAYMENT_TRANSACTIONS
   ```

**Deliverables**:
- ✅ Complete list of stored procedures (with source code)
- ✅ Database schema documentation
- ✅ Table relationship diagram
- ✅ Data dictionary for key tables

---

##### Day 9-10: Calculation Formula Extraction
**Owner**: Senior Java Developer #2 + Business Analyst

**Tasks**:
1. **Document Reducing Balance Method**
   ```
   Product: HOME_LOAN, PERSONAL_LOAN, VEHICLE_LOAN

   Formula: EMI = P × r × (1+r)^n / ((1+r)^n - 1)

   Where:
   - P = Principal loan amount
   - r = Monthly interest rate (annual_rate / 12 / 100)
   - n = Number of monthly installments

   Principal Component (month i) = EMI - Interest
   Interest Component (month i) = Outstanding Balance × r
   Outstanding Balance (month i) = Previous Balance - Principal Component

   Edge Cases:
   - Zero interest rate: EMI = P / n
   - Last installment: Adjust for rounding differences
   ```

2. **Document Flat Rate Method**
   ```
   Product: SHORT_TERM_PERSONAL_LOAN

   Formula: EMI = (P + (P × r × n)) / n

   Where:
   - P = Principal
   - r = Annual interest rate / 100
   - n = Tenure in years

   Interest = P × r × n (calculated upfront)
   Total Payment = P + Interest
   EMI = Total Payment / (n × 12)
   ```

3. **Document Bullet Payment Method**
   ```
   Product: BUSINESS_LOAN, CONSTRUCTION_LOAN

   Interest Payment (monthly) = P × (annual_rate / 12 / 100)
   Principal Payment (at maturity) = P

   Schedule:
   Month 1 to n-1: Interest only
   Month n: Interest + Principal
   ```

4. **Document Edge Case Logic**

   **Prepayment Handling**:
   ```
   User Options:
   1. Reduce Tenure (keep EMI same)
      - Recalculate tenure with new principal
      - Keep original EMI

   2. Reduce EMI (keep tenure same)
      - Recalculate EMI with new principal
      - Keep original tenure

   Process:
   - Apply prepayment to outstanding principal
   - Regenerate schedule from prepayment date
   - Maintain audit trail of original vs revised
   ```

   **Payment Holiday**:
   ```
   Interest Treatment Options:
   1. Capitalize Interest
      - Add unpaid interest to principal
      - Recalculate EMI from end of holiday

   2. Defer Interest
      - Add interest installments at end
      - Extend tenure

   3. Pay Interest Only
      - Collect interest during holiday
      - Resume normal EMI after
   ```

   **Interest Rate Change**:
   ```
   Application Methods:
   1. Prospective (Future EMIs)
      - Recalculate EMI with new rate
      - From effective date onwards

   2. Retrospective (Past EMIs)
      - Recalculate entire schedule
      - Adjust outstanding principal
      - May result in refund/additional payment
   ```

**Deliverables**:
- ✅ Detailed formula documentation for all methods
- ✅ Edge case handling specification
- ✅ Business rule documentation
- ✅ Sample calculations with expected results

---

##### Day 11-14: Test Data Extraction
**Owner**: QA Engineer #1 + Senior Java Developer #1

**Tasks**:
1. **Extract Sample Loans**
   ```sql
   -- Extract 100 representative loans
   SELECT * FROM (
     SELECT l.*, p.product_type, p.amortisation_method
     FROM loans l
     JOIN product_config p ON l.product_id = p.product_id
     WHERE l.status = 'ACTIVE'
     AND l.disbursement_date > SYSDATE - 365
     ORDER BY DBMS_RANDOM.VALUE
   ) WHERE ROWNUM <= 100;
   ```

   Coverage:
   - 30 Home Loans (reducing balance)
   - 25 Personal Loans (reducing balance)
   - 20 Vehicle Loans (reducing balance)
   - 10 Gold Loans (daily reducing)
   - 10 Business Loans (bullet payment)
   - 5 with prepayments
   - 5 with payment holidays
   - 5 with rate changes

2. **Extract Expected EMI Schedules**
   ```sql
   -- Get existing calculated schedules
   SELECT loan_id, installment_number, due_date,
          opening_balance, emi, principal, interest, closing_balance
   FROM emi_schedule
   WHERE loan_id IN (/* sample loan IDs */)
   ORDER BY loan_id, installment_number;
   ```

3. **Create Test Data File**
   ```json
   {
     "test_cases": [
       {
         "test_id": "TC001",
         "loan_id": "LN12345",
         "input": {
           "principal": 5000000.00,
           "interest_rate": 8.5,
           "tenure": 240,
           "product_type": "HOME_LOAN",
           "method": "REDUCING_BALANCE",
           "start_date": "2024-01-01"
         },
         "expected_output": {
           "emi": 43391.00,
           "total_interest": 5413840.00,
           "first_installment": {
             "principal": 8058.00,
             "interest": 35333.00,
             "closing_balance": 4991942.00
           },
           "last_installment": {
             "principal": 43086.00,
             "interest": 305.00,
             "closing_balance": 0.00
           }
         }
       }
       // ... 99 more test cases
     ]
   }
   ```

4. **Validate Test Data**
   - Run legacy system calculations
   - Verify results match database
   - Document any discrepancies

**Deliverables**:
- ✅ 100 test cases with input/expected output
- ✅ Test data JSON file
- ✅ Validation report (legacy vs database)
- ✅ Edge case test scenarios (20+ cases)

**Acceptance Criteria**:
- 100% of test cases have expected outputs
- All edge cases documented
- Test data covers all product types and methods

---

### Sprint 2: Domain Model & Repository Layer
**Duration**: Weeks 3-4
**Team**: Senior Java Developers + Java Developers

#### Week 3: Entity Classes & Database Configuration

##### Day 15-17: JPA Entity Implementation
**Owner**: Senior Java Developer #1

**Tasks**:
1. **Create Loan Entity**
   ```java
   package com.lms.amortisation.model.entity;

   import jakarta.persistence.*;
   import lombok.AllArgsConstructor;
   import lombok.Builder;
   import lombok.Data;
   import lombok.NoArgsConstructor;
   import java.math.BigDecimal;
   import java.time.LocalDate;

   @Entity
   @Table(name = "LOANS", schema = "LMS")
   @Data
   @Builder
   @NoArgsConstructor
   @AllArgsConstructor
   public class Loan {

       @Id
       @Column(name = "LOAN_ID", length = 20)
       private String loanId;

       @Column(name = "CUSTOMER_ID", nullable = false)
       private String customerId;

       @Column(name = "PRODUCT_ID", nullable = false)
       private String productId;

       @Column(name = "PRINCIPAL_AMOUNT", precision = 15, scale = 2, nullable = false)
       private BigDecimal principalAmount;

       @Column(name = "INTEREST_RATE", precision = 5, scale = 2, nullable = false)
       private BigDecimal interestRate;

       @Column(name = "TENURE_MONTHS", nullable = false)
       private Integer tenureMonths;

       @Column(name = "DISBURSEMENT_DATE", nullable = false)
       private LocalDate disbursementDate;

       @Column(name = "MATURITY_DATE", nullable = false)
       private LocalDate maturityDate;

       @Column(name = "EMI_AMOUNT", precision = 15, scale = 2)
       private BigDecimal emiAmount;

       @Column(name = "STATUS", length = 20, nullable = false)
       private String status;

       @Column(name = "CREATED_DATE", nullable = false)
       private LocalDate createdDate;

       @Column(name = "MODIFIED_DATE")
       private LocalDate modifiedDate;

       // Relationship to product config
       @ManyToOne(fetch = FetchType.LAZY)
       @JoinColumn(name = "PRODUCT_ID", insertable = false, updatable = false)
       private ProductConfig productConfig;
   }
   ```

2. **Create ProductConfig Entity**
   ```java
   @Entity
   @Table(name = "PRODUCT_CONFIG", schema = "LMS")
   @Data
   @Builder
   @NoArgsConstructor
   @AllArgsConstructor
   public class ProductConfig {

       @Id
       @Column(name = "PRODUCT_ID", length = 20)
       private String productId;

       @Column(name = "PRODUCT_NAME", length = 100, nullable = false)
       private String productName;

       @Column(name = "PRODUCT_TYPE", length = 50, nullable = false)
       private String productType;

       @Column(name = "AMORTISATION_METHOD", length = 50, nullable = false)
       private String amortisationMethod;

       @Column(name = "MIN_PRINCIPAL", precision = 15, scale = 2)
       private BigDecimal minPrincipal;

       @Column(name = "MAX_PRINCIPAL", precision = 15, scale = 2)
       private BigDecimal maxPrincipal;

       @Column(name = "MIN_TENURE")
       private Integer minTenure;

       @Column(name = "MAX_TENURE")
       private Integer maxTenure;

       @Column(name = "DEFAULT_RATE", precision = 5, scale = 2)
       private BigDecimal defaultRate;

       @Column(name = "ACTIVE", nullable = false)
       private Boolean active;
   }
   ```

3. **Create InterestRateMaster Entity**
   ```java
   @Entity
   @Table(name = "INTEREST_RATE_MASTER", schema = "LMS")
   @Data
   @Builder
   @NoArgsConstructor
   @AllArgsConstructor
   public class InterestRateMaster {

       @Id
       @Column(name = "RATE_ID", length = 20)
       private String rateId;

       @Column(name = "PRODUCT_ID", nullable = false)
       private String productId;

       @Column(name = "RATE_TYPE", length = 20, nullable = false)
       private String rateType; // FIXED, FLOATING, SPECIAL

       @Column(name = "BASE_RATE", precision = 5, scale = 2, nullable = false)
       private BigDecimal baseRate;

       @Column(name = "SPREAD", precision = 5, scale = 2)
       private BigDecimal spread;

       @Column(name = "EFFECTIVE_DATE", nullable = false)
       private LocalDate effectiveDate;

       @Column(name = "END_DATE")
       private LocalDate endDate;

       @Column(name = "ACTIVE", nullable = false)
       private Boolean active;
   }
   ```

4. **Create LoanEvent Entity**
   ```java
   @Entity
   @Table(name = "LOAN_EVENTS", schema = "LMS")
   @Data
   @Builder
   @NoArgsConstructor
   @AllArgsConstructor
   public class LoanEvent {

       @Id
       @Column(name = "EVENT_ID", length = 20)
       private String eventId;

       @Column(name = "LOAN_ID", nullable = false)
       private String loanId;

       @Column(name = "EVENT_TYPE", length = 50, nullable = false)
       private String eventType; // PREPAYMENT, PAYMENT_HOLIDAY, RATE_CHANGE

       @Column(name = "EVENT_DATE", nullable = false)
       private LocalDate eventDate;

       @Column(name = "AMOUNT", precision = 15, scale = 2)
       private BigDecimal amount;

       @Column(name = "PARAMETERS", columnDefinition = "CLOB")
       private String parameters; // JSON for additional data

       @Column(name = "PROCESSED", nullable = false)
       private Boolean processed;

       @Column(name = "CREATED_DATE", nullable = false)
       private LocalDate createdDate;
   }
   ```

5. **Create CalculationAuditLog Entity**
   ```java
   @Entity
   @Table(name = "CALCULATION_AUDIT_LOG", schema = "LMS")
   @Data
   @Builder
   @NoArgsConstructor
   @AllArgsConstructor
   public class CalculationAuditLog {

       @Id
       @Column(name = "AUDIT_ID", length = 50)
       private String auditId;

       @Column(name = "LOAN_ID", nullable = false)
       private String loanId;

       @Column(name = "REQUEST_ID", nullable = false)
       private String requestId;

       @Column(name = "CALCULATION_TYPE", length = 50, nullable = false)
       private String calculationType;

       @Column(name = "INPUT_PARAMETERS", columnDefinition = "CLOB")
       private String inputParameters;

       @Column(name = "AMORTISATION_METHOD", length = 50)
       private String amortisationMethod;

       @Column(name = "FORMULA", length = 500)
       private String formula;

       @Column(name = "PRINCIPAL", precision = 15, scale = 2)
       private BigDecimal principal;

       @Column(name = "INTEREST_RATE", precision = 5, scale = 2)
       private BigDecimal interestRate;

       @Column(name = "TENURE")
       private Integer tenure;

       @Column(name = "OUTPUT_SCHEDULE", columnDefinition = "CLOB")
       private String outputSchedule;

       @Column(name = "CALCULATED_BY", length = 100)
       private String calculatedBy;

       @Column(name = "CALCULATED_AT", nullable = false)
       private java.time.Instant calculatedAt;

       @Column(name = "SOURCE_IP", length = 50)
       private String sourceIp;

       @Column(name = "REGULATORY_VERSION", length = 50)
       private String regulatoryVersion;

       @Column(name = "AUDIT_TRAIL", columnDefinition = "CLOB")
       private String auditTrail;

       @Column(name = "RETENTION_UNTIL", nullable = false)
       private java.time.Instant retentionUntil;
   }
   ```

**Deliverables**:
- ✅ 5 JPA entity classes
- ✅ Proper annotations (@Entity, @Table, @Column)
- ✅ Lombok integration
- ✅ Relationships configured

**Acceptance Criteria**:
- Entities match Oracle schema exactly
- Build succeeds without errors
- Hibernate can generate DDL (for validation)

---

##### Day 18-19: Repository Layer Implementation
**Owner**: Java Developer #1

**Tasks**:
1. **Create LoanRepository**
   ```java
   package com.lms.amortisation.repository;

   import com.lms.amortisation.model.entity.Loan;
   import org.springframework.data.jpa.repository.JpaRepository;
   import org.springframework.data.jpa.repository.Query;
   import org.springframework.data.repository.query.Param;
   import org.springframework.stereotype.Repository;

   import java.time.LocalDate;
   import java.util.List;
   import java.util.Optional;

   @Repository
   public interface LoanRepository extends JpaRepository<Loan, String> {

       /**
        * Find loan by ID with product config
        */
       @Query("SELECT l FROM Loan l JOIN FETCH l.productConfig WHERE l.loanId = :loanId")
       Optional<Loan> findByIdWithProductConfig(@Param("loanId") String loanId);

       /**
        * Find active loans by product type
        */
       @Query("SELECT l FROM Loan l WHERE l.status = 'ACTIVE' AND l.productConfig.productType = :productType")
       List<Loan> findActiveByProductType(@Param("productType") String productType);

       /**
        * Find loans for batch processing
        */
       @Query("SELECT l FROM Loan l WHERE l.status = 'ACTIVE' ORDER BY l.loanId")
       List<Loan> findAllActiveForBatch();

       /**
        * Count active loans
        */
       @Query("SELECT COUNT(l) FROM Loan l WHERE l.status = 'ACTIVE'")
       long countActiveLoans();
   }
   ```

2. **Create ProductConfigRepository**
   ```java
   @Repository
   public interface ProductConfigRepository extends JpaRepository<ProductConfig, String> {

       /**
        * Find active product configurations
        */
       @Query("SELECT p FROM ProductConfig p WHERE p.active = true")
       List<ProductConfig> findAllActive();

       /**
        * Find by product type
        */
       List<ProductConfig> findByProductTypeAndActiveTrue(String productType);

       /**
        * Find by amortisation method
        */
       List<ProductConfig> findByAmortisationMethodAndActiveTrue(String amortisationMethod);
   }
   ```

3. **Create InterestRateMasterRepository**
   ```java
   @Repository
   public interface InterestRateMasterRepository extends JpaRepository<InterestRateMaster, String> {

       /**
        * Find current rate for product
        */
       @Query("SELECT i FROM InterestRateMaster i WHERE i.productId = :productId " +
              "AND i.effectiveDate <= :asOfDate " +
              "AND (i.endDate IS NULL OR i.endDate > :asOfDate) " +
              "AND i.active = true")
       Optional<InterestRateMaster> findCurrentRate(
           @Param("productId") String productId,
           @Param("asOfDate") LocalDate asOfDate
       );

       /**
        * Find rate history for product
        */
       @Query("SELECT i FROM InterestRateMaster i WHERE i.productId = :productId " +
              "ORDER BY i.effectiveDate DESC")
       List<InterestRateMaster> findRateHistory(@Param("productId") String productId);
   }
   ```

4. **Create LoanEventRepository**
   ```java
   @Repository
   public interface LoanEventRepository extends JpaRepository<LoanEvent, String> {

       /**
        * Find events for loan
        */
       List<LoanEvent> findByLoanIdOrderByEventDateAsc(String loanId);

       /**
        * Find unprocessed events
        */
       @Query("SELECT e FROM LoanEvent e WHERE e.processed = false ORDER BY e.eventDate")
       List<LoanEvent> findUnprocessedEvents();

       /**
        * Find events by type
        */
       List<LoanEvent> findByLoanIdAndEventType(String loanId, String eventType);
   }
   ```

5. **Create AuditLogRepository**
   ```java
   @Repository
   public interface AuditLogRepository extends JpaRepository<CalculationAuditLog, String> {

       /**
        * Find audit logs by loan ID
        */
       List<CalculationAuditLog> findByLoanIdOrderByCalculatedAtDesc(String loanId);

       /**
        * Find recent calculations
        */
       @Query("SELECT a FROM CalculationAuditLog a WHERE a.calculatedAt >= :since ORDER BY a.calculatedAt DESC")
       List<CalculationAuditLog> findRecentCalculations(@Param("since") java.time.Instant since);

       /**
        * Count calculations by date range
        */
       @Query("SELECT COUNT(a) FROM CalculationAuditLog a WHERE a.calculatedAt BETWEEN :start AND :end")
       long countByDateRange(@Param("start") java.time.Instant start, @Param("end") java.time.Instant end);
   }
   ```

**Deliverables**:
- ✅ 5 Spring Data JPA repositories
- ✅ Custom queries for complex operations
- ✅ Proper annotations

**Acceptance Criteria**:
- All repositories compile successfully
- No N+1 query issues (use JOIN FETCH)

---

##### Day 20-21: Database Configuration & Connection Pooling
**Owner**: DevOps Engineer + Senior Java Developer #2

**Tasks**:
1. **Create DatabaseConfig.java**
   ```java
   package com.lms.amortisation.config;

   import com.zaxxer.hikari.HikariConfig;
   import com.zaxxer.hikari.HikariDataSource;
   import org.springframework.beans.factory.annotation.Value;
   import org.springframework.context.annotation.Bean;
   import org.springframework.context.annotation.Configuration;
   import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
   import org.springframework.orm.jpa.JpaTransactionManager;
   import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
   import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
   import org.springframework.transaction.PlatformTransactionManager;
   import org.springframework.transaction.annotation.EnableTransactionManagement;

   import javax.sql.DataSource;
   import java.util.Properties;

   @Configuration
   @EnableTransactionManagement
   @EnableJpaRepositories(basePackages = "com.lms.amortisation.repository")
   public class DatabaseConfig {

       @Value("${spring.datasource.oracle.url}")
       private String jdbcUrl;

       @Value("${spring.datasource.oracle.username}")
       private String username;

       @Value("${spring.datasource.oracle.password}")
       private String password;

       @Bean
       public DataSource dataSource() {
           HikariConfig config = new HikariConfig();

           // Connection settings
           config.setJdbcUrl(jdbcUrl);
           config.setUsername(username);
           config.setPassword(password);
           config.setDriverClassName("oracle.jdbc.OracleDriver");

           // Pool settings
           config.setMaximumPoolSize(50);
           config.setMinimumIdle(10);
           config.setConnectionTimeout(20000); // 20 seconds
           config.setIdleTimeout(300000); // 5 minutes
           config.setMaxLifetime(1200000); // 20 minutes
           config.setLeakDetectionThreshold(60000); // 1 minute
           config.setPoolName("OracleHikariPool");

           // Oracle-specific settings
           config.addDataSourceProperty("cachePrepStmts", "true");
           config.addDataSourceProperty("prepStmtCacheSize", "250");
           config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
           config.addDataSourceProperty("oracle.net.CONNECT_TIMEOUT", "20000");
           config.addDataSourceProperty("oracle.jdbc.ReadTimeout", "60000");

           // Connection test
           config.setConnectionTestQuery("SELECT 1 FROM DUAL");

           return new HikariDataSource(config);
       }

       @Bean
       public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
           LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
           em.setDataSource(dataSource);
           em.setPackagesToScan("com.lms.amortisation.model.entity");

           HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
           em.setJpaVendorAdapter(vendorAdapter);

           Properties properties = new Properties();
           properties.setProperty("hibernate.dialect", "org.hibernate.dialect.Oracle12cDialect");
           properties.setProperty("hibernate.hbm2ddl.auto", "none");
           properties.setProperty("hibernate.show_sql", "false");
           properties.setProperty("hibernate.format_sql", "true");
           properties.setProperty("hibernate.jdbc.batch_size", "50");
           properties.setProperty("hibernate.jdbc.fetch_size", "100");
           properties.setProperty("hibernate.order_inserts", "true");
           properties.setProperty("hibernate.order_updates", "true");
           properties.setProperty("hibernate.jdbc.batch_versioned_data", "true");

           em.setJpaProperties(properties);

           return em;
       }

       @Bean
       public PlatformTransactionManager transactionManager(
           LocalContainerEntityManagerFactoryBean entityManagerFactory) {
           JpaTransactionManager transactionManager = new JpaTransactionManager();
           transactionManager.setEntityManagerFactory(entityManagerFactory.getObject());
           return transactionManager;
       }
   }
   ```

2. **Set Up RDS Proxy** (via AWS Console or Terraform)
   ```hcl
   # infrastructure/terraform/rds-proxy.tf
   resource "aws_db_proxy" "oracle_proxy" {
     name                   = "amortisation-oracle-proxy"
     debug_logging          = false
     engine_family          = "ORACLE"
     idle_client_timeout    = 1800
     require_tls            = true
     role_arn               = aws_iam_role.rds_proxy_role.arn
     vpc_subnet_ids         = aws_subnet.private[*].id

     auth {
       auth_scheme = "SECRETS"
       iam_auth    = "DISABLED"
       secret_arn  = aws_secretsmanager_secret.oracle_credentials.arn
     }

     tags = {
       Name = "amortisation-oracle-proxy"
     }
   }

   resource "aws_db_proxy_default_target_group" "oracle" {
     db_proxy_name = aws_db_proxy.oracle_proxy.name

     connection_pool_config {
       max_connections_percent      = 100
       max_idle_connections_percent = 50
       connection_borrow_timeout    = 120
     }
   }
   ```

3. **Configure AWS Secrets Manager**
   ```bash
   # Store Oracle credentials
   aws secretsmanager create-secret \
     --name amortisation/oracle/credentials \
     --secret-string '{"username":"lms_user","password":"CHANGE_ME"}'
   ```

4. **Test Database Connectivity**
   ```java
   // Create integration test
   @SpringBootTest
   @TestPropertySource(locations = "classpath:application-test.yml")
   class DatabaseConnectionTest {

       @Autowired
       private DataSource dataSource;

       @Autowired
       private LoanRepository loanRepository;

       @Test
       void testDatabaseConnection() throws SQLException {
           try (Connection conn = dataSource.getConnection()) {
               assertTrue(conn.isValid(5));
           }
       }

       @Test
       void testQueryExecution() {
           long count = loanRepository.count();
           assertTrue(count >= 0);
       }

       @Test
       void testConnectionPoolSize() {
           HikariDataSource hikariDS = (HikariDataSource) dataSource;
           assertEquals(50, hikariDS.getMaximumPoolSize());
           assertEquals(10, hikariDS.getMinimumIdle());
       }
   }
   ```

**Deliverables**:
- ✅ DatabaseConfig class with HikariCP
- ✅ RDS Proxy configured
- ✅ Secrets Manager integration
- ✅ Connection pool monitoring

**Acceptance Criteria**:
- Connection pool creates successfully
- Can execute queries against Oracle
- Connection timeout < 20ms
- Query execution < 200ms
- Pool metrics visible in actuator

---

#### Week 4: Repository Testing & Optimization

##### Day 22-23: Repository Unit Tests
**Owner**: QA Engineer #1 + Java Developer #2

**Tasks**:
1. **Set Up Test Infrastructure**
   ```java
   // Use TestContainers for Oracle
   @Testcontainers
   @SpringBootTest
   @TestPropertySource(locations = "classpath:application-test.yml")
   class RepositoryTestBase {

       @Container
       static OracleContainer oracle = new OracleContainer("gvenzl/oracle-xe:21-slim")
           .withDatabaseName("testdb")
           .withUsername("test")
           .withPassword("test");

       @DynamicPropertySource
       static void setProperties(DynamicPropertyRegistry registry) {
           registry.add("spring.datasource.oracle.url", oracle::getJdbcUrl);
           registry.add("spring.datasource.oracle.username", oracle::getUsername);
           registry.add("spring.datasource.oracle.password", oracle::getPassword);
       }
   }
   ```

2. **Write LoanRepository Tests**
   ```java
   @SpringBootTest
   @Transactional
   class LoanRepositoryTest extends RepositoryTestBase {

       @Autowired
       private LoanRepository loanRepository;

       @Autowired
       private ProductConfigRepository productConfigRepository;

       @BeforeEach
       void setUp() {
           // Create test product config
           ProductConfig product = ProductConfig.builder()
               .productId("PROD001")
               .productName("Home Loan")
               .productType("HOME_LOAN")
               .amortisationMethod("REDUCING_BALANCE")
               .active(true)
               .build();
           productConfigRepository.save(product);
       }

       @Test
       void testSaveLoan() {
           Loan loan = Loan.builder()
               .loanId("LN001")
               .customerId("CUST001")
               .productId("PROD001")
               .principalAmount(new BigDecimal("5000000.00"))
               .interestRate(new BigDecimal("8.5"))
               .tenureMonths(240)
               .disbursementDate(LocalDate.now())
               .maturityDate(LocalDate.now().plusMonths(240))
               .status("ACTIVE")
               .createdDate(LocalDate.now())
               .build();

           Loan saved = loanRepository.save(loan);

           assertNotNull(saved);
           assertEquals("LN001", saved.getLoanId());
       }

       @Test
       void testFindByIdWithProductConfig() {
           // Create and save loan
           Loan loan = createTestLoan("LN002");
           loanRepository.save(loan);

           // Fetch with product config
           Optional<Loan> found = loanRepository.findByIdWithProductConfig("LN002");

           assertTrue(found.isPresent());
           assertNotNull(found.get().getProductConfig());
           assertEquals("Home Loan", found.get().getProductConfig().getProductName());
       }

       @Test
       void testFindActiveByProductType() {
           // Create multiple loans
           loanRepository.save(createTestLoan("LN003"));
           loanRepository.save(createTestLoan("LN004"));

           List<Loan> loans = loanRepository.findActiveByProductType("HOME_LOAN");

           assertFalse(loans.isEmpty());
           assertTrue(loans.size() >= 2);
       }

       @Test
       void testCountActiveLoans() {
           loanRepository.save(createTestLoan("LN005"));
           loanRepository.save(createTestLoan("LN006"));

           long count = loanRepository.countActiveLoans();

           assertTrue(count >= 2);
       }

       private Loan createTestLoan(String loanId) {
           return Loan.builder()
               .loanId(loanId)
               .customerId("CUST001")
               .productId("PROD001")
               .principalAmount(new BigDecimal("5000000.00"))
               .interestRate(new BigDecimal("8.5"))
               .tenureMonths(240)
               .disbursementDate(LocalDate.now())
               .maturityDate(LocalDate.now().plusMonths(240))
               .status("ACTIVE")
               .createdDate(LocalDate.now())
               .build();
       }
   }
   ```

3. **Write Tests for Other Repositories**
   - ProductConfigRepositoryTest
   - InterestRateMasterRepositoryTest
   - LoanEventRepositoryTest
   - AuditLogRepositoryTest

4. **Measure Test Coverage**
   ```bash
   mvn clean test jacoco:report
   # Target: >90% coverage for repository layer
   ```

**Deliverables**:
- ✅ 50+ repository unit tests
- ✅ TestContainers integration
- ✅ >90% code coverage

**Acceptance Criteria**:
- All tests pass
- Code coverage >90%
- Tests run in <2 minutes

---

##### Day 24-28: Performance Testing & Optimization
**Owner**: Senior Java Developer #1 + DBA

**Tasks**:
1. **Create Performance Test Suite**
   ```java
   @SpringBootTest
   class RepositoryPerformanceTest extends RepositoryTestBase {

       @Autowired
       private LoanRepository loanRepository;

       @Test
       void testBulkFetchPerformance() {
           // Create 1000 test loans
           for (int i = 0; i < 1000; i++) {
               loanRepository.save(createTestLoan("LN" + i));
           }

           // Measure fetch time
           long start = System.currentTimeMillis();
           List<Loan> loans = loanRepository.findAllActiveForBatch();
           long duration = System.currentTimeMillis() - start;

           assertEquals(1000, loans.size());
           assertTrue(duration < 1000, "Fetch should complete in <1s, took " + duration + "ms");
       }

       @Test
       void testConnectionPoolUnderLoad() throws InterruptedException {
           ExecutorService executor = Executors.newFixedThreadPool(50);
           CountDownLatch latch = new CountDownLatch(50);

           for (int i = 0; i < 50; i++) {
               executor.submit(() -> {
                   try {
                       loanRepository.findById("LN001");
                   } finally {
                       latch.countDown();
                   }
               });
           }

           boolean completed = latch.await(10, TimeUnit.SECONDS);
           assertTrue(completed, "All queries should complete within 10s");

           executor.shutdown();
       }
   }
   ```

2. **Analyze Query Performance**
   ```sql
   -- Enable query logging
   SET hibernate.show_sql=true
   SET hibernate.format_sql=true
   SET hibernate.use_sql_comments=true

   -- Check for N+1 queries
   -- Monitor slow queries (>100ms)
   ```

3. **Add Database Indexes** (if missing)
   ```sql
   -- Indexes for common queries
   CREATE INDEX idx_loans_status ON LOANS(STATUS);
   CREATE INDEX idx_loans_product_id ON LOANS(PRODUCT_ID);
   CREATE INDEX idx_loans_disbursement_date ON LOANS(DISBURSEMENT_DATE);
   CREATE INDEX idx_loan_events_loan_id ON LOAN_EVENTS(LOAN_ID, EVENT_DATE);
   CREATE INDEX idx_audit_loan_calculated_at ON CALCULATION_AUDIT_LOG(LOAN_ID, CALCULATED_AT);
   ```

4. **Optimize HikariCP Settings**
   ```yaml
   # Fine-tune based on load tests
   spring:
     datasource:
       hikari:
         maximum-pool-size: 50  # Adjust based on concurrent load
         minimum-idle: 10
         connection-timeout: 20000
         idle-timeout: 300000
         max-lifetime: 1200000
   ```

**Deliverables**:
- ✅ Performance test suite
- ✅ Query performance analysis
- ✅ Database indexes added
- ✅ Optimized connection pool settings

**Acceptance Criteria**:
- Fetch 1000 loans: <1 second
- 50 concurrent queries: <10 seconds
- No N+1 query issues
- Connection pool efficiency >80%

---

**Sprint 2 Deliverables Summary**:
- ✅ 5 JPA entity classes
- ✅ 5 Spring Data repositories
- ✅ Database configuration with HikariCP
- ✅ RDS Proxy integration
- ✅ 50+ unit tests (>90% coverage)
- ✅ Performance test suite
- ✅ Optimized queries and indexes

**Sprint 2 Acceptance Criteria**:
- All entities map to Oracle tables
- All repositories functional
- Database connectivity stable
- Query performance meets targets
- Test coverage >90%

---

### Sprint 3: Calculation Engine Implementation
**Duration**: Weeks 5-7
**Team**: Senior Java Developers + Java Developers

#### Week 5: Calculator Framework & Reducing Balance Implementation

##### Day 29-31: Calculation Engine Architecture
**Owner**: Tech Lead + Senior Java Developer #1

**Tasks**:
1. **Design Calculator Framework**
   - Already created: `AmortisationCalculator.java` interface
   - Already created: `ReducingBalanceCalculator.java` implementation

2. **Create CalculatorFactory**
   ```java
   package com.lms.amortisation.service.calculator;

   import com.lms.amortisation.exception.CalculationException;
   import org.springframework.stereotype.Component;
   import lombok.RequiredArgsConstructor;
   import java.util.List;

   @Component
   @RequiredArgsConstructor
   public class CalculatorFactory {

       private final List<AmortisationCalculator> calculators;

       /**
        * Get appropriate calculator for given method
        */
       public AmortisationCalculator getCalculator(String method) {
           return calculators.stream()
               .filter(calc -> calc.supports(method))
               .findFirst()
               .orElseThrow(() -> new CalculationException(
                   "No calculator found for method: " + method
               ));
       }

       /**
        * Get all supported calculation methods
        */
       public List<String> getSupportedMethods() {
           return calculators.stream()
               .map(AmortisationCalculator::getCalculatorName)
               .toList();
       }
   }
   ```

3. **Create Financial Utility Functions**
   ```java
   package com.lms.amortisation.util;

   import java.math.BigDecimal;
   import java.math.RoundingMode;

   public class FinancialUtils {

       private static final int PRECISION = 15;
       private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

       /**
        * Calculate EMI using reducing balance method
        */
       public static BigDecimal calculateEMI(BigDecimal principal,
                                             BigDecimal monthlyRate,
                                             int tenure) {
           if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
               return principal.divide(
                   BigDecimal.valueOf(tenure), 2, ROUNDING
               );
           }

           BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
           BigDecimal powerN = power(onePlusRate, tenure);

           BigDecimal numerator = principal
               .multiply(monthlyRate)
               .multiply(powerN);

           BigDecimal denominator = powerN.subtract(BigDecimal.ONE);

           return numerator.divide(denominator, 2, ROUNDING);
       }

       /**
        * Calculate power (base^exponent) with high precision
        */
       public static BigDecimal power(BigDecimal base, int exponent) {
           if (exponent == 0) {
               return BigDecimal.ONE;
           }

           BigDecimal result = base;
           for (int i = 1; i < exponent; i++) {
               result = result.multiply(base);
           }

           return result.setScale(PRECISION, ROUNDING);
       }

       /**
        * Calculate monthly rate from annual rate
        */
       public static BigDecimal getMonthlyRate(BigDecimal annualRate) {
           return annualRate
               .divide(BigDecimal.valueOf(12), PRECISION, ROUNDING)
               .divide(BigDecimal.valueOf(100), PRECISION, ROUNDING);
       }

       /**
        * Calculate NPV (Net Present Value)
        */
       public static BigDecimal calculateNPV(List<BigDecimal> cashFlows,
                                             BigDecimal discountRate) {
           BigDecimal npv = BigDecimal.ZERO;

           for (int i = 0; i < cashFlows.size(); i++) {
               BigDecimal discount = BigDecimal.ONE
                   .add(discountRate)
                   .pow(i + 1);

               npv = npv.add(
                   cashFlows.get(i).divide(discount, PRECISION, ROUNDING)
               );
           }

           return npv.setScale(2, ROUNDING);
       }

       /**
        * Calculate IRR (Internal Rate of Return)
        * Using Newton-Raphson method
        */
       public static BigDecimal calculateIRR(List<BigDecimal> cashFlows) {
           // Implementation of Newton-Raphson for IRR
           // This is complex - simplified version
           BigDecimal guess = new BigDecimal("0.1");
           BigDecimal tolerance = new BigDecimal("0.00001");
           int maxIterations = 100;

           for (int i = 0; i < maxIterations; i++) {
               BigDecimal npv = calculateNPV(cashFlows, guess);

               if (npv.abs().compareTo(tolerance) < 0) {
                   return guess.setScale(4, ROUNDING);
               }

               // Calculate derivative (simplified)
               BigDecimal derivative = calculateNPVDerivative(cashFlows, guess);
               guess = guess.subtract(
                   npv.divide(derivative, PRECISION, ROUNDING)
               );
           }

           return guess.setScale(4, ROUNDING);
       }

       private static BigDecimal calculateNPVDerivative(
           List<BigDecimal> cashFlows, BigDecimal rate) {
           BigDecimal derivative = BigDecimal.ZERO;

           for (int i = 0; i < cashFlows.size(); i++) {
               int period = i + 1;
               BigDecimal discount = BigDecimal.ONE
                   .add(rate)
                   .pow(period + 1);

               derivative = derivative.subtract(
                   cashFlows.get(i)
                       .multiply(BigDecimal.valueOf(period))
                       .divide(discount, PRECISION, ROUNDING)
               );
           }

           return derivative;
       }
   }
   ```

4. **Create DateUtils**
   ```java
   package com.lms.amortisation.util;

   import java.time.DayOfWeek;
   import java.time.LocalDate;
   import java.time.temporal.ChronoUnit;
   import java.util.HashSet;
   import java.util.Set;

   public class DateUtils {

       private static final Set<LocalDate> HOLIDAYS = new HashSet<>();

       /**
        * Add months to date (handling month-end scenarios)
        */
       public static LocalDate addMonths(LocalDate date, int months) {
           return date.plusMonths(months);
       }

       /**
        * Check if date is business day
        */
       public static boolean isBusinessDay(LocalDate date) {
           DayOfWeek dayOfWeek = date.getDayOfWeek();

           // Weekend check
           if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
               return false;
           }

           // Holiday check
           return !HOLIDAYS.contains(date);
       }

       /**
        * Get next business day
        */
       public static LocalDate getNextBusinessDay(LocalDate date) {
           LocalDate next = date.plusDays(1);

           while (!isBusinessDay(next)) {
               next = next.plusDays(1);
           }

           return next;
       }

       /**
        * Calculate days between dates
        */
       public static long daysBetween(LocalDate start, LocalDate end) {
           return ChronoUnit.DAYS.between(start, end);
       }

       /**
        * Calculate months between dates
        */
       public static long monthsBetween(LocalDate start, LocalDate end) {
           return ChronoUnit.MONTHS.between(start, end);
       }

       /**
        * Load holidays from database/config
        */
       public static void loadHolidays(Set<LocalDate> holidays) {
           HOLIDAYS.clear();
           HOLIDAYS.addAll(holidays);
       }
   }
   ```

**Deliverables**:
- ✅ Calculator framework (interface + factory)
- ✅ Financial utility functions
- ✅ Date utility functions
- ✅ Architecture documentation

**Acceptance Criteria**:
- Factory can instantiate calculators
- Utility functions tested with sample data
- Code follows clean code principles

---

##### Day 32-35: Complete Calculator Implementations
**Owner**: Senior Java Developer #1 & #2, Java Developer #1 & #2

**Tasks**:

1. **Implement FlatRateCalculator**
   ```java
   package com.lms.amortisation.service.calculator;

   @Slf4j
   @Component
   public class FlatRateCalculator implements AmortisationCalculator {

       @Override
       public EMISchedule calculate(CalculationRequest request) {
           log.debug("Starting flat rate calculation for loanId: {}", request.getLoanId());

           BigDecimal principal = request.getPrincipal();
           BigDecimal annualRate = request.getInterestRate();
           int tenureMonths = request.getTenure();
           LocalDate startDate = request.getStartDate();

           // Calculate total interest (upfront)
           BigDecimal tenureYears = new BigDecimal(tenureMonths)
               .divide(BigDecimal.valueOf(12), 15, RoundingMode.HALF_UP);

           BigDecimal totalInterest = principal
               .multiply(annualRate.divide(BigDecimal.valueOf(100), 15, RoundingMode.HALF_UP))
               .multiply(tenureYears);

           // Calculate total payment
           BigDecimal totalPayment = principal.add(totalInterest);

           // Calculate EMI
           BigDecimal emi = totalPayment
               .divide(BigDecimal.valueOf(tenureMonths), 2, RoundingMode.HALF_UP);

           // Interest and principal per installment
           BigDecimal interestPerMonth = totalInterest
               .divide(BigDecimal.valueOf(tenureMonths), 2, RoundingMode.HALF_UP);

           BigDecimal principalPerMonth = principal
               .divide(BigDecimal.valueOf(tenureMonths), 2, RoundingMode.HALF_UP);

           // Generate schedule
           List<Installment> schedule = new ArrayList<>();
           BigDecimal outstandingPrincipal = principal;
           BigDecimal cumulativePrincipal = BigDecimal.ZERO;
           BigDecimal cumulativeInterest = BigDecimal.ZERO;

           for (int i = 1; i <= tenureMonths; i++) {
               // Adjust last installment for rounding
               if (i == tenureMonths) {
                   principalPerMonth = outstandingPrincipal;
               }

               cumulativePrincipal = cumulativePrincipal.add(principalPerMonth);
               cumulativeInterest = cumulativeInterest.add(interestPerMonth);

               BigDecimal closingBalance = outstandingPrincipal.subtract(principalPerMonth);

               Installment installment = Installment.builder()
                   .installmentNumber(i)
                   .dueDate(startDate.plusMonths(i))
                   .openingBalance(outstandingPrincipal.setScale(2, RoundingMode.HALF_UP))
                   .emi(emi)
                   .principal(principalPerMonth.setScale(2, RoundingMode.HALF_UP))
                   .interest(interestPerMonth)
                   .closingBalance(closingBalance.setScale(2, RoundingMode.HALF_UP))
                   .cumulativePrincipal(cumulativePrincipal.setScale(2, RoundingMode.HALF_UP))
                   .cumulativeInterest(cumulativeInterest.setScale(2, RoundingMode.HALF_UP))
                   .build();

               schedule.add(installment);
               outstandingPrincipal = closingBalance;
           }

           String auditTrail = buildAuditTrail(principal, annualRate, tenureMonths, emi);

           return EMISchedule.builder()
               .loanId(request.getLoanId())
               .emi(emi)
               .totalInterest(totalInterest.setScale(2, RoundingMode.HALF_UP))
               .totalPayment(totalPayment.setScale(2, RoundingMode.HALF_UP))
               .schedule(schedule)
               .auditTrail(auditTrail)
               .calculationMethod(AmortisationMethod.FLAT_RATE.name())
               .build();
       }

       private String buildAuditTrail(BigDecimal principal, BigDecimal annualRate,
                                      int tenure, BigDecimal emi) {
           return String.format(
               "Amortisation Method: FLAT_RATE | " +
               "Formula: EMI = (P + (P × r × n)) / n | " +
               "Parameters: P=%s, Annual Rate=%s%%, Tenure=%d months | " +
               "Calculated EMI: %s | " +
               "Regulatory Version: RBI-2024-v1",
               principal, annualRate, tenure, emi
           );
       }

       @Override
       public boolean supports(String method) {
           return AmortisationMethod.FLAT_RATE.name().equalsIgnoreCase(method);
       }

       @Override
       public String getCalculatorName() {
           return AmortisationMethod.FLAT_RATE.name();
       }
   }
   ```

2. **Implement BulletPaymentCalculator**
   ```java
   @Slf4j
   @Component
   public class BulletPaymentCalculator implements AmortisationCalculator {

       @Override
       public EMISchedule calculate(CalculationRequest request) {
           log.debug("Starting bullet payment calculation for loanId: {}", request.getLoanId());

           BigDecimal principal = request.getPrincipal();
           BigDecimal annualRate = request.getInterestRate();
           int tenureMonths = request.getTenure();
           LocalDate startDate = request.getStartDate();

           // Calculate monthly interest rate
           BigDecimal monthlyRate = annualRate
               .divide(BigDecimal.valueOf(12), 15, RoundingMode.HALF_UP)
               .divide(BigDecimal.valueOf(100), 15, RoundingMode.HALF_UP);

           // Interest per month
           BigDecimal monthlyInterest = principal
               .multiply(monthlyRate)
               .setScale(2, RoundingMode.HALF_UP);

           // Generate schedule
           List<Installment> schedule = new ArrayList<>();
           BigDecimal cumulativeInterest = BigDecimal.ZERO;

           for (int i = 1; i <= tenureMonths; i++) {
               BigDecimal principalComponent;
               BigDecimal emi;
               BigDecimal closingBalance;

               if (i < tenureMonths) {
                   // Interest-only installments
                   principalComponent = BigDecimal.ZERO;
                   emi = monthlyInterest;
                   closingBalance = principal;
               } else {
                   // Final installment: Principal + Interest
                   principalComponent = principal;
                   emi = principal.add(monthlyInterest);
                   closingBalance = BigDecimal.ZERO;
               }

               cumulativeInterest = cumulativeInterest.add(monthlyInterest);

               Installment installment = Installment.builder()
                   .installmentNumber(i)
                   .dueDate(startDate.plusMonths(i))
                   .openingBalance(principal.setScale(2, RoundingMode.HALF_UP))
                   .emi(emi)
                   .principal(principalComponent.setScale(2, RoundingMode.HALF_UP))
                   .interest(monthlyInterest)
                   .closingBalance(closingBalance.setScale(2, RoundingMode.HALF_UP))
                   .cumulativePrincipal(principalComponent.setScale(2, RoundingMode.HALF_UP))
                   .cumulativeInterest(cumulativeInterest.setScale(2, RoundingMode.HALF_UP))
                   .build();

               schedule.add(installment);
           }

           BigDecimal totalInterest = monthlyInterest.multiply(BigDecimal.valueOf(tenureMonths));
           BigDecimal totalPayment = principal.add(totalInterest);

           String auditTrail = buildAuditTrail(principal, annualRate, tenureMonths, monthlyInterest);

           return EMISchedule.builder()
               .loanId(request.getLoanId())
               .emi(monthlyInterest) // Regular EMI (interest only)
               .totalInterest(totalInterest.setScale(2, RoundingMode.HALF_UP))
               .totalPayment(totalPayment.setScale(2, RoundingMode.HALF_UP))
               .schedule(schedule)
               .auditTrail(auditTrail)
               .calculationMethod(AmortisationMethod.BULLET_PAYMENT.name())
               .build();
       }

       private String buildAuditTrail(BigDecimal principal, BigDecimal annualRate,
                                      int tenure, BigDecimal monthlyInterest) {
           return String.format(
               "Amortisation Method: BULLET_PAYMENT | " +
               "Formula: Monthly Interest = P × (r/12/100), Principal at Maturity | " +
               "Parameters: P=%s, Annual Rate=%s%%, Tenure=%d months | " +
               "Monthly Interest: %s | " +
               "Regulatory Version: RBI-2024-v1",
               principal, annualRate, tenure, monthlyInterest
           );
       }

       @Override
       public boolean supports(String method) {
           return AmortisationMethod.BULLET_PAYMENT.name().equalsIgnoreCase(method);
       }

       @Override
       public String getCalculatorName() {
           return AmortisationMethod.BULLET_PAYMENT.name();
       }
   }
   ```

3. **Implement DailyReducingCalculator** (for Gold Loans)
   ```java
   @Slf4j
   @Component
   public class DailyReducingCalculator implements AmortisationCalculator {

       @Override
       public EMISchedule calculate(CalculationRequest request) {
           log.debug("Starting daily reducing calculation for loanId: {}", request.getLoanId());

           BigDecimal principal = request.getPrincipal();
           BigDecimal annualRate = request.getInterestRate();
           int tenureMonths = request.getTenure();
           LocalDate startDate = request.getStartDate();

           // Daily interest rate
           BigDecimal dailyRate = annualRate
               .divide(BigDecimal.valueOf(365), 15, RoundingMode.HALF_UP)
               .divide(BigDecimal.valueOf(100), 15, RoundingMode.HALF_UP);

           // Generate schedule
           List<Installment> schedule = new ArrayList<>();
           BigDecimal outstandingBalance = principal;
           BigDecimal cumulativePrincipal = BigDecimal.ZERO;
           BigDecimal cumulativeInterest = BigDecimal.ZERO;

           for (int i = 1; i <= tenureMonths; i++) {
               LocalDate previousDate = (i == 1) ? startDate : startDate.plusMonths(i - 1);
               LocalDate currentDate = startDate.plusMonths(i);

               // Calculate days in this period
               long days = ChronoUnit.DAYS.between(previousDate, currentDate);

               // Interest for this month
               BigDecimal interest = outstandingBalance
                   .multiply(dailyRate)
                   .multiply(BigDecimal.valueOf(days))
                   .setScale(2, RoundingMode.HALF_UP);

               // Principal component (simplified - equal installments)
               BigDecimal principalComponent = principal
                   .divide(BigDecimal.valueOf(tenureMonths), 2, RoundingMode.HALF_UP);

               // Adjust last installment
               if (i == tenureMonths) {
                   principalComponent = outstandingBalance;
               }

               BigDecimal emi = principalComponent.add(interest);
               BigDecimal closingBalance = outstandingBalance.subtract(principalComponent);

               cumulativePrincipal = cumulativePrincipal.add(principalComponent);
               cumulativeInterest = cumulativeInterest.add(interest);

               Installment installment = Installment.builder()
                   .installmentNumber(i)
                   .dueDate(currentDate)
                   .openingBalance(outstandingBalance.setScale(2, RoundingMode.HALF_UP))
                   .emi(emi.setScale(2, RoundingMode.HALF_UP))
                   .principal(principalComponent.setScale(2, RoundingMode.HALF_UP))
                   .interest(interest)
                   .closingBalance(closingBalance.setScale(2, RoundingMode.HALF_UP))
                   .cumulativePrincipal(cumulativePrincipal.setScale(2, RoundingMode.HALF_UP))
                   .cumulativeInterest(cumulativeInterest.setScale(2, RoundingMode.HALF_UP))
                   .build();

               schedule.add(installment);
               outstandingBalance = closingBalance;
           }

           BigDecimal totalInterest = cumulativeInterest;
           BigDecimal totalPayment = principal.add(totalInterest);

           String auditTrail = buildAuditTrail(principal, annualRate, tenureMonths);

           return EMISchedule.builder()
               .loanId(request.getLoanId())
               .emi(null) // Variable EMI in daily reducing
               .totalInterest(totalInterest.setScale(2, RoundingMode.HALF_UP))
               .totalPayment(totalPayment.setScale(2, RoundingMode.HALF_UP))
               .schedule(schedule)
               .auditTrail(auditTrail)
               .calculationMethod(AmortisationMethod.DAILY_REDUCING.name())
               .build();
       }

       private String buildAuditTrail(BigDecimal principal, BigDecimal annualRate, int tenure) {
           return String.format(
               "Amortisation Method: DAILY_REDUCING | " +
               "Formula: Interest = Outstanding × (r/365/100) × Days | " +
               "Parameters: P=%s, Annual Rate=%s%%, Tenure=%d months | " +
               "Regulatory Version: RBI-2024-v1",
               principal, annualRate, tenure
           );
       }

       @Override
       public boolean supports(String method) {
           return AmortisationMethod.DAILY_REDUCING.name().equalsIgnoreCase(method);
       }

       @Override
       public String getCalculatorName() {
           return AmortisationMethod.DAILY_REDUCING.name();
       }
   }
   ```

**Deliverables**:
- ✅ FlatRateCalculator
- ✅ BulletPaymentCalculator
- ✅ DailyReducingCalculator
- ✅ Unit tests for all calculators

**Acceptance Criteria**:
- All calculators implement interface correctly
- Calculations match extracted formulas
- Test coverage >95%

---

#### Week 6-7: Edge Case Handlers

##### Day 36-40: Prepayment Handler
**Owner**: Senior Java Developer #2 + Java Developer #1

**Tasks**:

1. **Create PrepaymentHandler**
   ```java
   package com.lms.amortisation.service.edgecase;

   import com.lms.amortisation.model.dto.*;
   import com.lms.amortisation.model.enums.PrepaymentOption;
   import lombok.RequiredArgsConstructor;
   import lombok.extern.slf4j.Slf4j;
   import org.springframework.stereotype.Component;

   import java.math.BigDecimal;
   import java.math.RoundingMode;
   import java.time.LocalDate;
   import java.util.ArrayList;
   import java.util.List;

   @Slf4j
   @Component
   @RequiredArgsConstructor
   public class PrepaymentHandler {

       /**
        * Apply prepayment to existing schedule
        *
        * @param originalSchedule Original EMI schedule
        * @param prepaymentAmount Amount being prepaid
        * @param prepaymentDate Date of prepayment
        * @param option REDUCE_TENURE or REDUCE_EMI
        * @return Revised schedule after prepayment
        */
       public PrepaymentResult applyPrepayment(
           EMISchedule originalSchedule,
           BigDecimal prepaymentAmount,
           LocalDate prepaymentDate,
           PrepaymentOption option) {

           log.info("Applying prepayment of {} on {} with option {}",
               prepaymentAmount, prepaymentDate, option);

           // Find the installment number for prepayment date
           int prepaymentInstallment = findInstallmentForDate(
               originalSchedule.getSchedule(), prepaymentDate
           );

           // Get outstanding principal at prepayment date
           Installment currentInstallment = originalSchedule.getSchedule()
               .get(prepaymentInstallment - 1);

           BigDecimal outstandingPrincipal = currentInstallment.getClosingBalance();
           BigDecimal newPrincipal = outstandingPrincipal.subtract(prepaymentAmount);

           // Validate prepayment amount
           if (prepaymentAmount.compareTo(outstandingPrincipal) > 0) {
               throw new IllegalArgumentException(
                   "Prepayment amount cannot exceed outstanding principal"
               );
           }

           // Calculate impact and revised schedule based on option
           PrepaymentResult result;
           if (option == PrepaymentOption.REDUCE_TENURE) {
               result = reduceTenure(
                   originalSchedule, newPrincipal, prepaymentInstallment, prepaymentDate
               );
           } else {
               result = reduceEMI(
                   originalSchedule, newPrincipal, prepaymentInstallment, prepaymentDate
               );
           }

           // Set prepayment details
           result.setPrepaymentAmount(prepaymentAmount);
           result.setPrepaymentDate(prepaymentDate);
           result.setOriginalSchedule(originalSchedule.getSchedule());

           return result;
       }

       private PrepaymentResult reduceTenure(
           EMISchedule originalSchedule,
           BigDecimal newPrincipal,
           int fromInstallment,
           LocalDate prepaymentDate) {

           BigDecimal emi = originalSchedule.getEmi();
           BigDecimal monthlyRate = extractMonthlyRate(originalSchedule);

           // Calculate new tenure
           int newTenure = calculateNewTenure(newPrincipal, emi, monthlyRate);
           int tenureReduction = (originalSchedule.getSchedule().size() - fromInstallment + 1) - newTenure;

           // Generate revised schedule from prepayment date
           List<Installment> revisedSchedule = generateRevisedSchedule(
               newPrincipal, emi, monthlyRate, newTenure, prepaymentDate, fromInstallment
           );

           // Copy unchanged installments from original schedule
           List<Installment> completeSchedule = new ArrayList<>(
               originalSchedule.getSchedule().subList(0, fromInstallment - 1)
           );
           completeSchedule.addAll(revisedSchedule);

           // Calculate interest savings
           BigDecimal originalInterest = calculateRemainingInterest(
               originalSchedule.getSchedule(), fromInstallment
           );
           BigDecimal newInterest = calculateTotalInterest(revisedSchedule);
           BigDecimal interestSaving = originalInterest.subtract(newInterest);

           return PrepaymentResult.builder()
               .revisedSchedule(completeSchedule)
               .tenureChange(-tenureReduction)
               .emiChange(BigDecimal.ZERO)
               .interestSaving(interestSaving)
               .option(PrepaymentOption.REDUCE_TENURE)
               .build();
       }

       private PrepaymentResult reduceEMI(
           EMISchedule originalSchedule,
           BigDecimal newPrincipal,
           int fromInstallment,
           LocalDate prepaymentDate) {

           int remainingTenure = originalSchedule.getSchedule().size() - fromInstallment + 1;
           BigDecimal monthlyRate = extractMonthlyRate(originalSchedule);

           // Calculate new EMI
           BigDecimal newEMI = calculateEMI(newPrincipal, monthlyRate, remainingTenure);
           BigDecimal emiReduction = originalSchedule.getEmi().subtract(newEMI);

           // Generate revised schedule
           List<Installment> revisedSchedule = generateRevisedSchedule(
               newPrincipal, newEMI, monthlyRate, remainingTenure, prepaymentDate, fromInstallment
           );

           // Copy unchanged installments
           List<Installment> completeSchedule = new ArrayList<>(
               originalSchedule.getSchedule().subList(0, fromInstallment - 1)
           );
           completeSchedule.addAll(revisedSchedule);

           // Calculate interest savings
           BigDecimal originalInterest = calculateRemainingInterest(
               originalSchedule.getSchedule(), fromInstallment
           );
           BigDecimal newInterest = calculateTotalInterest(revisedSchedule);
           BigDecimal interestSaving = originalInterest.subtract(newInterest);

           return PrepaymentResult.builder()
               .revisedSchedule(completeSchedule)
               .tenureChange(0)
               .emiChange(emiReduction.negate())
               .interestSaving(interestSaving)
               .option(PrepaymentOption.REDUCE_EMI)
               .build();
       }

       // Helper methods

       private int findInstallmentForDate(List<Installment> schedule, LocalDate date) {
           for (Installment inst : schedule) {
               if (inst.getDueDate().isAfter(date) || inst.getDueDate().isEqual(date)) {
                   return inst.getInstallmentNumber();
               }
           }
           return schedule.size();
       }

       private BigDecimal extractMonthlyRate(EMISchedule schedule) {
           // Extract from audit trail or recalculate
           // Simplified: assuming standard reducing balance
           Installment first = schedule.getSchedule().get(0);
           BigDecimal interest = first.getInterest();
           BigDecimal principal = first.getOpeningBalance();
           return interest.divide(principal, 15, RoundingMode.HALF_UP);
       }

       private int calculateNewTenure(BigDecimal principal, BigDecimal emi, BigDecimal monthlyRate) {
           if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
               return principal.divide(emi, 0, RoundingMode.UP).intValue();
           }

           // Formula: n = log((EMI)/(EMI - P*r)) / log(1+r)
           BigDecimal pTimesR = principal.multiply(monthlyRate);
           double numerator = Math.log(emi.divide(emi.subtract(pTimesR), 15, RoundingMode.HALF_UP).doubleValue());
           double denominator = Math.log(BigDecimal.ONE.add(monthlyRate).doubleValue());

           return (int) Math.ceil(numerator / denominator);
       }

       private BigDecimal calculateEMI(BigDecimal principal, BigDecimal monthlyRate, int tenure) {
           if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
               return principal.divide(BigDecimal.valueOf(tenure), 2, RoundingMode.HALF_UP);
           }

           BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
           BigDecimal powerN = onePlusRate.pow(tenure);

           BigDecimal numerator = principal.multiply(monthlyRate).multiply(powerN);
           BigDecimal denominator = powerN.subtract(BigDecimal.ONE);

           return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
       }

       private List<Installment> generateRevisedSchedule(
           BigDecimal principal,
           BigDecimal emi,
           BigDecimal monthlyRate,
           int tenure,
           LocalDate startDate,
           int startInstallmentNumber) {

           List<Installment> schedule = new ArrayList<>();
           BigDecimal outstandingBalance = principal;
           BigDecimal cumulativePrincipal = BigDecimal.ZERO;
           BigDecimal cumulativeInterest = BigDecimal.ZERO;

           for (int i = 0; i < tenure; i++) {
               BigDecimal interest = outstandingBalance
                   .multiply(monthlyRate)
                   .setScale(2, RoundingMode.HALF_UP);

               BigDecimal principalComponent = emi.subtract(interest);

               // Adjust last installment
               if (i == tenure - 1) {
                   principalComponent = outstandingBalance;
                   interest = emi.subtract(principalComponent);
               }

               cumulativePrincipal = cumulativePrincipal.add(principalComponent);
               cumulativeInterest = cumulativeInterest.add(interest);

               BigDecimal closingBalance = outstandingBalance.subtract(principalComponent);

               Installment installment = Installment.builder()
                   .installmentNumber(startInstallmentNumber + i)
                   .dueDate(startDate.plusMonths(i + 1))
                   .openingBalance(outstandingBalance.setScale(2, RoundingMode.HALF_UP))
                   .emi(emi)
                   .principal(principalComponent.setScale(2, RoundingMode.HALF_UP))
                   .interest(interest)
                   .closingBalance(closingBalance.setScale(2, RoundingMode.HALF_UP))
                   .cumulativePrincipal(cumulativePrincipal.setScale(2, RoundingMode.HALF_UP))
                   .cumulativeInterest(cumulativeInterest.setScale(2, RoundingMode.HALF_UP))
                   .build();

               schedule.add(installment);
               outstandingBalance = closingBalance;
           }

           return schedule;
       }

       private BigDecimal calculateRemainingInterest(List<Installment> schedule, int fromInstallment) {
           return schedule.stream()
               .skip(fromInstallment - 1)
               .map(Installment::getInterest)
               .reduce(BigDecimal.ZERO, BigDecimal::add);
       }

       private BigDecimal calculateTotalInterest(List<Installment> schedule) {
           return schedule.stream()
               .map(Installment::getInterest)
               .reduce(BigDecimal.ZERO, BigDecimal::add);
       }
   }
   ```

2. **Create PrepaymentResult DTO**
   ```java
   package com.lms.amortisation.model.dto;

   import lombok.AllArgsConstructor;
   import lombok.Builder;
   import lombok.Data;
   import lombok.NoArgsConstructor;
   import java.math.BigDecimal;
   import java.time.LocalDate;
   import java.util.List;

   @Data
   @Builder
   @NoArgsConstructor
   @AllArgsConstructor
   public class PrepaymentResult {
       private BigDecimal prepaymentAmount;
       private LocalDate prepaymentDate;
       private PrepaymentOption option;
       private List<Installment> originalSchedule;
       private List<Installment> revisedSchedule;
       private int tenureChange; // Negative means reduction
       private BigDecimal emiChange; // Negative means reduction
       private BigDecimal interestSaving;
   }
   ```

3. **Create PrepaymentOption Enum**
   ```java
   package com.lms.amortisation.model.enums;

   public enum PrepaymentOption {
       REDUCE_TENURE,
       REDUCE_EMI
   }
   ```

4. **Write Comprehensive Tests**
   ```java
   @SpringBootTest
   class PrepaymentHandlerTest {

       @Autowired
       private PrepaymentHandler prepaymentHandler;

       @Test
       void testPrepaymentReduceTenure() {
           // Create original schedule
           EMISchedule original = createSampleSchedule();

           // Apply prepayment
           PrepaymentResult result = prepaymentHandler.applyPrepayment(
               original,
               new BigDecimal("100000.00"),
               LocalDate.of(2025, 6, 1),
               PrepaymentOption.REDUCE_TENURE
           );

           // Verify
           assertTrue(result.getTenureChange() < 0);
           assertEquals(BigDecimal.ZERO, result.getEmiChange());
           assertTrue(result.getInterestSaving().compareTo(BigDecimal.ZERO) > 0);
       }

       @Test
       void testPrepaymentReduceEMI() {
           EMISchedule original = createSampleSchedule();

           PrepaymentResult result = prepaymentHandler.applyPrepayment(
               original,
               new BigDecimal("100000.00"),
               LocalDate.of(2025, 6, 1),
               PrepaymentOption.REDUCE_EMI
           );

           assertEquals(0, result.getTenureChange());
           assertTrue(result.getEmiChange().compareTo(BigDecimal.ZERO) < 0);
           assertTrue(result.getInterestSaving().compareTo(BigDecimal.ZERO) > 0);
       }
   }
   ```

**Deliverables**:
- ✅ PrepaymentHandler with both options
- ✅ PrepaymentResult DTO
- ✅ Comprehensive unit tests
- ✅ Integration tests with calculators

---

##### Day 41-45: Payment Holiday & Rate Change Handlers
**Owner**: Java Developer #2 + QA Engineer #2

**Tasks**:

1. **Create PaymentHolidayHandler**
   ```java
   @Slf4j
   @Component
   @RequiredArgsConstructor
   public class PaymentHolidayHandler {

       /**
        * Apply payment holiday to schedule
        *
        * @param originalSchedule Original EMI schedule
        * @param holidayStartDate Start date of holiday
        * @param holidayEndDate End date of holiday
        * @param interestTreatment CAPITALIZE, DEFER, or PAY_ONLY_INTEREST
        * @return Revised schedule with holiday applied
        */
       public PaymentHolidayResult applyPaymentHoliday(
           EMISchedule originalSchedule,
           LocalDate holidayStartDate,
           LocalDate holidayEndDate,
           InterestTreatment interestTreatment) {

           log.info("Applying payment holiday from {} to {} with treatment {}",
               holidayStartDate, holidayEndDate, interestTreatment);

           // Implementation details...
           // 1. Find installments in holiday period
           // 2. Apply interest treatment
           // 3. Regenerate schedule post-holiday
           // 4. Calculate impact

           return PaymentHolidayResult.builder()
               .revisedSchedule(revisedSchedule)
               .holidayMonths(holidayMonths)
               .capitalizedInterest(capitalizedInterest)
               .tenureExtension(tenureExtension)
               .build();
       }
   }
   ```

2. **Create RateChangeHandler**
   ```java
   @Slf4j
   @Component
   @RequiredArgsConstructor
   public class RateChangeHandler {

       /**
        * Apply interest rate change to schedule
        *
        * @param originalSchedule Original EMI schedule
        * @param newRate New interest rate
        * @param effectiveDate Effective date of rate change
        * @param method PROSPECTIVE or RETROSPECTIVE
        * @return Revised schedule with rate change
        */
       public RateChangeResult applyRateChange(
           EMISchedule originalSchedule,
           BigDecimal newRate,
           LocalDate effectiveDate,
           RateChangeMethod method) {

           log.info("Applying rate change to {} from {} using {} method",
               newRate, effectiveDate, method);

           // Implementation details...
           // 1. Find installment for effective date
           // 2. If PROSPECTIVE: recalculate from that date
           // 3. If RETROSPECTIVE: recalculate entire schedule
           // 4. Calculate impact

           return RateChangeResult.builder()
               .revisedSchedule(revisedSchedule)
               .rateChange(rateChange)
               .emiChange(emiChange)
               .interestImpact(interestImpact)
               .build();
       }
   }
   ```

3. **Create EdgeCaseOrchestrator**
   ```java
   @Slf4j
   @Component
   @RequiredArgsConstructor
   public class EdgeCaseOrchestrator {

       private final PrepaymentHandler prepaymentHandler;
       private final PaymentHolidayHandler paymentHolidayHandler;
       private final RateChangeHandler rateChangeHandler;

       /**
        * Apply multiple edge cases in sequence
        *
        * @param originalSchedule Original schedule
        * @param events List of loan events to apply
        * @return Final revised schedule
        */
       public EdgeCaseResult applyEdgeCases(
           EMISchedule originalSchedule,
           List<LoanEvent> events) {

           log.info("Applying {} edge case events", events.size());

           EMISchedule current = originalSchedule;
           List<String> appliedEvents = new ArrayList<>();

           // Sort events by date
           events.sort(Comparator.comparing(LoanEvent::getEventDate));

           for (LoanEvent event : events) {
               switch (event.getEventType()) {
                   case "PREPAYMENT":
                       // Apply prepayment
                       break;
                   case "PAYMENT_HOLIDAY":
                       // Apply payment holiday
                       break;
                   case "RATE_CHANGE":
                       // Apply rate change
                       break;
               }
               appliedEvents.add(event.getEventType());
           }

           return EdgeCaseResult.builder()
               .finalSchedule(current)
               .appliedEvents(appliedEvents)
               .build();
       }
   }
   ```

**Deliverables**:
- ✅ PaymentHolidayHandler
- ✅ RateChangeHandler
- ✅ EdgeCaseOrchestrator
- ✅ Result DTOs
- ✅ Unit tests for all handlers

**Acceptance Criteria**:
- All edge cases handled correctly
- Test coverage >90%
- Results match business rules

---

**Sprint 3 Deliverables Summary**:
- ✅ 4 calculation engines (Reducing Balance, Flat Rate, Bullet Payment, Daily Reducing)
- ✅ CalculatorFactory for strategy pattern
- ✅ Financial & Date utilities
- ✅ 3 edge case handlers (Prepayment, Payment Holiday, Rate Change)
- ✅ EdgeCaseOrchestrator
- ✅ 100+ unit tests (>95% coverage)

**Sprint 3 Acceptance Criteria**:
- All calculations match reverse-engineered formulas
- Edge cases handled per business rules
- Test coverage >95%
- Performance: calculation time <100ms

---

## TO BE CONTINUED...

This plan continues for 15 more sprints covering:
- Service Layer & Caching (Sprint 4)
- REST API & Security (Sprint 5)
- Audit & Compliance (Sprint 6)
- Observability (Sprint 7)
- Monolith Adapter (Sprint 8)
- Testing & Shadow Mode (Sprint 9)
- Deployment (Sprint 10)
- Batch Processing (Sprint 11-12)
- Performance Optimization (Sprint 13)
- Full Migration (Sprint 14-15)
- Event-Driven Architecture (Sprint 16-18)

**Total Pages**: This detailed plan would be 200+ pages when fully expanded.

---

## Quick Reference

### Key Milestones
- **Week 2**: Reverse engineering complete
- **Week 4**: Repository layer complete
- **Week 7**: Calculation engine complete
- **Week 12**: APIs & security complete
- **Week 16**: Shadow mode validated
- **Week 18**: 10% production traffic (Phase 1 done)
- **Week 27**: 100% production traffic (Phase 2 done)
- **Week 36**: Event-driven complete (Phase 3 done)

### Daily Standup Questions
1. What did you complete yesterday?
2. What are you working on today?
3. Any blockers?
4. Any risks or dependencies?

### Weekly Review Focus
- Week 1-2: Infrastructure setup
- Week 3-4: Data layer
- Week 5-7: Business logic
- Week 8-12: Integration & security
- Week 13-18: Testing & deployment

---

**Document Version**: 1.0
**Last Updated**: 2025-11-02
**Next Review**: Weekly during implementation

**Generated with Claude Code** 🤖
