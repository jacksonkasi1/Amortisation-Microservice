# Amortisation Microservice - Implementation Plan

## Project Structure

```
amortisation-microservice/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── lms/
│   │   │           └── amortisation/
│   │   │               ├── AmortisationApplication.java
│   │   │               ├── config/
│   │   │               │   ├── SecurityConfig.java
│   │   │               │   ├── DatabaseConfig.java
│   │   │               │   ├── RedisConfig.java
│   │   │               │   ├── BatchConfig.java
│   │   │               │   └── ObservabilityConfig.java
│   │   │               ├── controller/
│   │   │               │   ├── AmortisationController.java
│   │   │               │   ├── BatchController.java
│   │   │               │   └── HealthController.java
│   │   │               ├── service/
│   │   │               │   ├── AmortisationService.java
│   │   │               │   ├── calculator/
│   │   │               │   │   ├── AmortisationCalculator.java (interface)
│   │   │               │   │   ├── ReducingBalanceCalculator.java
│   │   │               │   │   ├── FlatRateCalculator.java
│   │   │               │   │   ├── BulletPaymentCalculator.java
│   │   │               │   │   └── CalculatorFactory.java
│   │   │               │   ├── edgecase/
│   │   │               │   │   ├── PrepaymentHandler.java
│   │   │               │   │   ├── PaymentHolidayHandler.java
│   │   │               │   │   ├── RateChangeHandler.java
│   │   │               │   │   └── EdgeCaseOrchestrator.java
│   │   │               │   ├── batch/
│   │   │               │   │   ├── MonthEndAccrualJob.java
│   │   │               │   │   ├── DayEndProcessingJob.java
│   │   │               │   │   └── BatchStatusService.java
│   │   │               │   └── audit/
│   │   │               │       ├── AuditService.java
│   │   │               │       └── ComplianceValidator.java
│   │   │               ├── repository/
│   │   │               │   ├── LoanRepository.java
│   │   │               │   ├── ProductConfigRepository.java
│   │   │               │   ├── InterestRateMasterRepository.java
│   │   │               │   ├── LoanEventRepository.java
│   │   │               │   └── AuditLogRepository.java
│   │   │               ├── model/
│   │   │               │   ├── entity/
│   │   │               │   │   ├── Loan.java
│   │   │               │   │   ├── ProductConfig.java
│   │   │               │   │   ├── InterestRateMaster.java
│   │   │               │   │   ├── LoanEvent.java
│   │   │               │   │   └── CalculationAuditLog.java
│   │   │               │   ├── dto/
│   │   │               │   │   ├── CalculationRequest.java
│   │   │               │   │   ├── CalculationResponse.java
│   │   │               │   │   ├── EMISchedule.java
│   │   │               │   │   ├── Installment.java
│   │   │               │   │   └── BatchJobRequest.java
│   │   │               │   └── enums/
│   │   │               │       ├── ProductType.java
│   │   │               │       ├── AmortisationMethod.java
│   │   │               │       ├── EdgeCaseType.java
│   │   │               │       └── BatchJobStatus.java
│   │   │               ├── exception/
│   │   │               │   ├── GlobalExceptionHandler.java
│   │   │               │   ├── CalculationException.java
│   │   │               │   ├── DataNotFoundException.java
│   │   │               │   └── ComplianceException.java
│   │   │               └── util/
│   │   │                   ├── DateUtils.java
│   │   │                   ├── FinancialUtils.java
│   │   │                   └── ValidationUtils.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       └── db/
│   │           └── changelog/
│   │               └── db.changelog-master.xml
│   └── test/
│       └── java/
│           └── com/
│               └── lms/
│                   └── amortisation/
│                       ├── service/
│                       │   └── calculator/
│                       │       ├── ReducingBalanceCalculatorTest.java
│                       │       └── ...
│                       ├── integration/
│                       │   ├── AmortisationIntegrationTest.java
│                       │   └── BatchProcessingIntegrationTest.java
│                       └── performance/
│                           └── LoadTest.java
├── infrastructure/
│   ├── kubernetes/
│   │   ├── deployment.yml
│   │   ├── service.yml
│   │   ├── hpa.yml
│   │   ├── configmap.yml
│   │   ├── secrets.yml (template)
│   │   └── ingress.yml
│   ├── helm/
│   │   └── amortisation-service/
│   │       ├── Chart.yaml
│   │       ├── values.yaml
│   │       └── templates/
│   ├── terraform/
│   │   ├── eks-cluster.tf
│   │   ├── rds-proxy.tf
│   │   ├── redis-cluster.tf
│   │   ├── networking.tf
│   │   └── monitoring.tf
│   └── docker/
│       ├── Dockerfile
│       └── docker-compose.yml (for local dev)
├── monitoring/
│   ├── grafana/
│   │   └── dashboards/
│   │       ├── business-metrics.json
│   │       ├── technical-metrics.json
│   │       └── compliance-metrics.json
│   ├── prometheus/
│   │   ├── prometheus.yml
│   │   └── alerts.yml
│   └── cloudwatch/
│       └── alarms.tf
├── docs/
│   ├── API.md
│   ├── ARCHITECTURE.md
│   ├── RUNBOOK.md
│   └── COMPLIANCE.md
├── scripts/
│   ├── reverse-engineer/
│   │   └── extract-legacy-logic.sql
│   ├── migration/
│   │   └── shadow-mode-comparison.py
│   └── deployment/
│       └── deploy.sh
├── .github/
│   └── workflows/
│       ├── ci.yml
│       ├── cd-dev.yml
│       └── cd-prod.yml
├── pom.xml
├── README.md
└── .gitignore
```

---

## Phase 1: Foundation (Months 1-3)

### Sprint 1: Project Setup & Reverse Engineering (Weeks 1-2)

#### Tasks:
- [ ] **Environment Setup**
  - [ ] Create Git repository
  - [ ] Set up project structure (Maven/Gradle)
  - [ ] Configure Spring Boot 3.2 with Java 21
  - [ ] Set up local development environment (Docker Compose)
  - [ ] Configure IDE (IntelliJ/Eclipse)

- [ ] **Reverse Engineering**
  - [ ] Identify all Oracle stored procedures for amortisation
  - [ ] Extract PL/SQL code for each product type
  - [ ] Document amortisation formulas (Equal EMI, Reducing Balance, Flat Rate, Bullet)
  - [ ] Identify edge case handling (prepayments, holidays, rate changes)
  - [ ] Extract product configuration rules
  - [ ] Create test data set (100 sample loans with expected outputs)
  - [ ] Document dependencies on master data tables

- [ ] **Infrastructure Foundation**
  - [ ] Set up AWS account and networking (VPC, subnets, security groups)
  - [ ] Configure Direct Connect/VPN to on-premise
  - [ ] Test Oracle connectivity from AWS
  - [ ] Set up RDS Proxy for Oracle connections
  - [ ] Deploy Redis cluster (ElastiCache)

---

### Sprint 2: Core Domain Model & Repository Layer (Weeks 3-4)

#### Tasks:
- [ ] **Domain Models**
  - [ ] Create entity classes (Loan, ProductConfig, InterestRateMaster, LoanEvent)
  - [ ] Create DTOs (CalculationRequest, CalculationResponse, EMISchedule, Installment)
  - [ ] Create enums (ProductType, AmortisationMethod, EdgeCaseType)
  - [ ] Add validation annotations (JSR-303)

- [ ] **Repository Layer**
  - [ ] Implement JPA repositories for Oracle entities
  - [ ] Create custom queries for loan data retrieval
  - [ ] Implement product configuration repository
  - [ ] Implement interest rate master repository
  - [ ] Configure HikariCP connection pool
  - [ ] Write unit tests for repositories

- [ ] **Database Configuration**
  - [ ] Configure multi-datasource (Oracle)
  - [ ] Set up connection pooling (min: 10, max: 50)
  - [ ] Configure transaction management
  - [ ] Add database health checks
  - [ ] Configure retry logic for transient failures

---

### Sprint 3: Calculation Engine Implementation (Weeks 5-7)

#### Tasks:
- [ ] **Calculator Framework**
  - [ ] Design AmortisationCalculator interface
  - [ ] Implement ReducingBalanceCalculator
  - [ ] Implement FlatRateCalculator
  - [ ] Implement BulletPaymentCalculator
  - [ ] Implement CalculatorFactory (Strategy Pattern)
  - [ ] Add financial utility functions (IRR, NPV, PMT)

- [ ] **Core Calculation Logic**
  - [ ] EMI calculation algorithm
  - [ ] Installment schedule generation
  - [ ] Principal/Interest split calculation
  - [ ] Cumulative calculations
  - [ ] Date handling (business days, holidays)

- [ ] **Edge Case Handlers**
  - [ ] PrepaymentHandler (reduce tenure vs reduce EMI)
  - [ ] PaymentHolidayHandler (capitalize interest vs defer)
  - [ ] RateChangeHandler (prospective vs retrospective)
  - [ ] PartPaymentHandler
  - [ ] EdgeCaseOrchestrator (chain handlers)

- [ ] **Testing**
  - [ ] Unit tests for each calculator (100% coverage)
  - [ ] Test with extracted legacy test cases
  - [ ] Verify calculation accuracy (15 decimal precision)
  - [ ] Performance tests (calculation time < 100ms)

---

### Sprint 4: Service Layer & Caching (Weeks 8-9)

#### Tasks:
- [ ] **Service Layer**
  - [ ] Implement AmortisationService
  - [ ] Integrate calculator factory
  - [ ] Add business validation logic
  - [ ] Implement edge case orchestration
  - [ ] Add service-level error handling

- [ ] **Caching Strategy**
  - [ ] Configure Redis with Spring Cache
  - [ ] Implement cache-aside pattern for product configs
  - [ ] Implement write-through cache for interest rates
  - [ ] Add cache for calculation results (15 min TTL)
  - [ ] Implement cache warming on startup
  - [ ] Add cache eviction strategies
  - [ ] Monitor cache hit rate

- [ ] **Testing**
  - [ ] Service layer unit tests
  - [ ] Integration tests with Redis
  - [ ] Cache behavior tests
  - [ ] Performance tests with caching

---

### Sprint 5: REST API & Security (Weeks 10-11)

#### Tasks:
- [ ] **REST Controllers**
  - [ ] CalculationController (POST /calculate)
  - [ ] ScheduleController (GET /schedule/{loanId})
  - [ ] RecalculationController (POST /recalculate)
  - [ ] HealthController (GET /health, /ready)
  - [ ] Add OpenAPI documentation

- [ ] **Security Configuration**
  - [ ] Set up Spring Security
  - [ ] Configure OAuth2 resource server
  - [ ] Integrate with OAuth2 provider (Keycloak/Okta)
  - [ ] Implement JWT validation
  - [ ] Add method-level security (@PreAuthorize)
  - [ ] Configure CORS policies
  - [ ] Add rate limiting (Bucket4j)

- [ ] **Exception Handling**
  - [ ] Global exception handler
  - [ ] Custom exception classes
  - [ ] Error response DTOs
  - [ ] Logging for exceptions

- [ ] **Testing**
  - [ ] Controller unit tests (MockMvc)
  - [ ] Security tests (JWT validation)
  - [ ] API integration tests
  - [ ] Contract tests (Pact)

---

### Sprint 6: Audit & Compliance (Week 12)

#### Tasks:
- [ ] **Audit Service**
  - [ ] Create CalculationAuditLog entity
  - [ ] Implement AuditService
  - [ ] Log all calculation inputs/outputs
  - [ ] Log calculation methodology
  - [ ] Store step-by-step audit trail
  - [ ] Add user/system context

- [ ] **Compliance Validator**
  - [ ] Implement RBI guideline checks
  - [ ] Validate interest calculation methods
  - [ ] Verify payment allocation rules
  - [ ] Add compliance version tracking
  - [ ] Generate compliance reports

- [ ] **Data Retention**
  - [ ] Configure 7-year retention for audit logs
  - [ ] Implement archival strategy
  - [ ] Add GDPR compliance (if applicable)

---

### Sprint 7: Observability & Monitoring (Week 13)

#### Tasks:
- [ ] **Metrics**
  - [ ] Configure Micrometer with Prometheus
  - [ ] Add custom business metrics (calculations/hour)
  - [ ] Add technical metrics (latency, error rate)
  - [ ] Expose /actuator/prometheus endpoint

- [ ] **Distributed Tracing**
  - [ ] Configure OpenTelemetry
  - [ ] Add trace IDs to logs
  - [ ] Integrate with Jaeger/Zipkin
  - [ ] Trace database and cache calls

- [ ] **Logging**
  - [ ] Configure Logback with JSON encoder
  - [ ] Add correlation IDs
  - [ ] Log to CloudWatch Logs
  - [ ] Set up log aggregation (ELK/CloudWatch)

- [ ] **Dashboards**
  - [ ] Create Grafana dashboards
  - [ ] Business metrics panel
  - [ ] Technical metrics panel
  - [ ] Database metrics panel
  - [ ] Cache metrics panel

- [ ] **Alerts**
  - [ ] Configure CloudWatch alarms (latency, error rate)
  - [ ] Configure Prometheus alerts
  - [ ] Set up PagerDuty/Slack integration

---

### Sprint 8: Monolith Adapter Layer (Week 14)

#### Tasks:
- [ ] **.NET Adapter Implementation**
  - [ ] Create REST client in .NET monolith
  - [ ] Implement request/response mapping
  - [ ] Add OAuth2 token acquisition
  - [ ] Implement circuit breaker (Polly)
  - [ ] Add retry with exponential backoff
  - [ ] Implement fallback to legacy code

- [ ] **Feature Flag**
  - [ ] Add feature flag system (LaunchDarkly/ConfigCat)
  - [ ] Configure percentage-based rollout
  - [ ] Add kill switch for emergencies

- [ ] **Testing**
  - [ ] Integration tests (.NET ↔ Java)
  - [ ] Latency tests
  - [ ] Fallback scenario tests

---

### Sprint 9: Testing & Shadow Mode (Weeks 15-16)

#### Tasks:
- [ ] **Test Automation**
  - [ ] Expand unit test coverage (>90%)
  - [ ] Integration tests with TestContainers
  - [ ] End-to-end tests
  - [ ] Performance tests (300 TPS)
  - [ ] Load tests (Apache JMeter/Gatling)
  - [ ] Chaos engineering tests

- [ ] **Shadow Mode**
  - [ ] Deploy microservice to staging
  - [ ] Run parallel calculations (legacy vs new)
  - [ ] Build reconciliation tool
  - [ ] Compare results (100% match required)
  - [ ] Analyze performance differences
  - [ ] Fix discrepancies

- [ ] **Security Testing**
  - [ ] OWASP ZAP scan
  - [ ] Penetration testing
  - [ ] Dependency vulnerability scan (Snyk)

---

### Sprint 10: Deployment & Canary Release (Weeks 17-18)

#### Tasks:
- [ ] **Infrastructure as Code**
  - [ ] Terraform scripts for EKS cluster
  - [ ] Helm chart for microservice
  - [ ] Kubernetes manifests (Deployment, Service, HPA, Ingress)
  - [ ] Configure secrets management (AWS Secrets Manager)

- [ ] **CI/CD Pipeline**
  - [ ] GitHub Actions workflow (build, test, scan)
  - [ ] Docker image build with Jib
  - [ ] Push to ECR
  - [ ] Deploy to dev/staging/prod
  - [ ] Automated rollback on failure

- [ ] **Production Deployment**
  - [ ] Deploy to production EKS
  - [ ] Configure ALB with SSL
  - [ ] Set up API Gateway (Kong)
  - [ ] Enable auto-scaling (3-20 pods)
  - [ ] Configure health checks

- [ ] **Canary Release**
  - [ ] Route 1% traffic to microservice
  - [ ] Monitor error rates and latency
  - [ ] Gradually increase to 10% → 25% → 50%
  - [ ] Monitor for 24 hours at each stage

---

## Phase 2: Optimization (Months 4-6)

### Sprint 11: Batch Processing Foundation (Weeks 19-21)

#### Tasks:
- [ ] **Spring Batch Setup**
  - [ ] Configure Spring Batch infrastructure
  - [ ] Design job repository (PostgreSQL)
  - [ ] Create batch job launcher
  - [ ] Implement job status tracking API

- [ ] **Month-End Accrual Job**
  - [ ] Design job flow (Reader → Processor → Writer)
  - [ ] Implement ItemReader (paginated Oracle queries)
  - [ ] Implement ItemProcessor (amortisation calculation)
  - [ ] Implement ItemWriter (write back to Oracle/callback)
  - [ ] Configure chunk size (1000)
  - [ ] Implement partitioning (10 parallel threads)

- [ ] **Day-End Processing Job**
  - [ ] Implement daily accrual job
  - [ ] Add job scheduling (Quartz/Spring Scheduler)
  - [ ] Configure job restartability

- [ ] **Batch Infrastructure**
  - [ ] Deploy dedicated batch pods (higher resources)
  - [ ] Configure batch job monitoring
  - [ ] Add job failure notifications
  - [ ] Implement job checkpointing

---

### Sprint 12: Performance Optimization (Weeks 22-24)

#### Tasks:
- [ ] **Database Optimization**
  - [ ] Analyze slow queries (query plan)
  - [ ] Add database indexes
  - [ ] Optimize HikariCP settings
  - [ ] Implement query result caching
  - [ ] Add read replicas (if needed)

- [ ] **Algorithm Optimization**
  - [ ] Profile calculation algorithms
  - [ ] Optimize loops and calculations
  - [ ] Use BigDecimal efficiently
  - [ ] Add calculation result memoization

- [ ] **Cache Optimization**
  - [ ] Analyze cache hit rates
  - [ ] Tune TTL values
  - [ ] Implement cache warming
  - [ ] Add distributed caching strategies

- [ ] **Load Testing**
  - [ ] Sustained load test (300 TPS for 1 hour)
  - [ ] Spike test (burst to 500 TPS)
  - [ ] Soak test (200 TPS for 24 hours)
  - [ ] Batch processing stress test (500K loans)

---

### Sprint 13: Full Traffic Migration (Weeks 25-27)

#### Tasks:
- [ ] **Gradual Rollout**
  - [ ] Increase to 75% traffic
  - [ ] Monitor for 1 week
  - [ ] Increase to 90% traffic
  - [ ] Monitor for 3 days
  - [ ] Route 100% traffic

- [ ] **Legacy Decommission**
  - [ ] Mark legacy code as deprecated
  - [ ] Remove feature flags
  - [ ] Archive legacy code
  - [ ] Update documentation

- [ ] **Operational Readiness**
  - [ ] Create runbook
  - [ ] Conduct incident response drills
  - [ ] Train operations team
  - [ ] Fine-tune alerts

---

## Phase 3: Event-Driven (Months 7-9)

### Sprint 14-18: CDC & Database Migration (Weeks 28-36)

#### Tasks:
- [ ] **CDC Pipeline**
  - [ ] Set up Debezium/Oracle GoldenGate
  - [ ] Configure Kafka/Kinesis cluster
  - [ ] Implement event schema
  - [ ] Build event consumer
  - [ ] Add idempotency handling

- [ ] **PostgreSQL Migration**
  - [ ] Design PostgreSQL schema
  - [ ] Set up RDS PostgreSQL
  - [ ] Implement dual-write mechanism
  - [ ] Sync historical data
  - [ ] Validate replication lag (<1 min)
  - [ ] Switch reads to PostgreSQL
  - [ ] Decommission Oracle dependency

- [ ] **Event-Driven Batch**
  - [ ] Implement Kafka-based batch processing
  - [ ] Add dead letter queue handling
  - [ ] Implement event replay mechanism

---

## Success Criteria Checklist

### Phase 1 Completion:
- [ ] 100% calculation parity with legacy system
- [ ] P95 latency < 5 seconds
- [ ] 99.99% availability
- [ ] Complete audit trail for all calculations
- [ ] 10% production traffic successfully migrated
- [ ] Zero high-severity incidents
- [ ] All security vulnerabilities resolved
- [ ] Compliance audit passed

### Phase 2 Completion:
- [ ] 100% production traffic on microservice
- [ ] Batch jobs complete within 6-hour window
- [ ] 500K loans processed without errors
- [ ] P95 latency < 3 seconds
- [ ] Legacy code decommissioned
- [ ] Operations team fully trained

### Phase 3 Completion:
- [ ] Zero Oracle queries for calculations
- [ ] CDC replication lag < 1 minute
- [ ] Event-driven batch processing operational
- [ ] PostgreSQL fully operational
- [ ] Horizontal scalability validated

---

## Team Structure & Roles

```
┌────────────────────────────────────────────────────┐
│                  Project Team                      │
├────────────────────────────────────────────────────┤
│ Role                    │ Headcount │ Responsibility
├─────────────────────────┼───────────┼───────────────
│ Tech Lead               │ 1         │ Architecture, code review
│ Senior Java Developer   │ 2         │ Core implementation
│ Java Developer          │ 2         │ Feature development
│ .NET Developer          │ 1         │ Adapter layer
│ DevOps Engineer         │ 1         │ Infrastructure, CI/CD
│ QA Engineer             │ 2         │ Testing, automation
│ DBA                     │ 1         │ Database optimization
│ Security Engineer       │ 0.5       │ Security review
│ Compliance Specialist   │ 0.5       │ RBI compliance
└────────────────────────────────────────────────────┘
Total: 11 FTE
```

---

## Cost Estimation (Monthly - Phase 1)

```
┌────────────────────────────────────────────────────┐
│ AWS Resource            │ Specification │ Cost/Month
├─────────────────────────┼───────────────┼───────────
│ EKS Cluster             │ Control plane │ $73
│ EC2 Instances (EKS)     │ 5 x t3.xlarge │ $750
│ RDS Proxy               │ 2 proxies     │ $130
│ ElastiCache Redis       │ cache.r6g.large│ $180
│ ALB                     │ 1 load balancer│ $25
│ Data Transfer (On-prem) │ 1 TB/month    │ $90
│ CloudWatch Logs         │ 100 GB/month  │ $50
│ Direct Connect          │ 1 Gbps        │ $300
├─────────────────────────┼───────────────┼───────────
│ TOTAL                   │               │ ~$1,600/mo
└────────────────────────────────────────────────────┘

Note: Excludes Direct Connect setup cost (~$5,000 one-time)
```

---

## Key Milestones

| Milestone | Week | Deliverable |
|-----------|------|-------------|
| M1: Project Kickoff | Week 1 | Repository setup, team onboarded |
| M2: Logic Documented | Week 2 | All amortisation logic reverse-engineered |
| M3: Core Engine Ready | Week 7 | All calculators implemented and tested |
| M4: API Ready | Week 11 | REST API with security fully functional |
| M5: Shadow Mode | Week 16 | 100% calculation parity validated |
| M6: Canary Release | Week 18 | 10% production traffic |
| M7: Batch Ready | Week 21 | Month-end jobs operational |
| M8: Full Migration | Week 27 | 100% traffic, legacy decommissioned |
| M9: Event-Driven | Week 36 | CDC and PostgreSQL operational |

---

## Dependencies & Prerequisites

### Before Starting:
1. **Approvals**
   - [ ] Budget approval (~$50K for 6 months)
   - [ ] Architecture review board approval
   - [ ] Security team signoff
   - [ ] Compliance team signoff

2. **Infrastructure**
   - [ ] AWS account provisioned
   - [ ] Direct Connect/VPN established
   - [ ] Firewall rules approved
   - [ ] OAuth2 provider available

3. **Access**
   - [ ] Oracle database read access
   - [ ] Production monolith code access
   - [ ] EKS cluster admin access
   - [ ] CI/CD pipeline access

4. **Tooling**
   - [ ] GitHub/GitLab repository
   - [ ] Jira/Project tracking tool
   - [ ] Confluence/Documentation wiki
   - [ ] Monitoring tools (Grafana, DataDog)
