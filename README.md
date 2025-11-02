# Amortisation Microservice

A scalable microservice for calculating loan amortisation schedules, extracted from a legacy .NET monolith LMS system.

## 🎯 Project Overview

This microservice provides:
- **Real-time EMI calculation** for various loan products
- **Batch processing** for month-end and day-end operations
- **Multiple amortisation methods** (Reducing Balance, Flat Rate, Bullet Payment)
- **Edge case handling** (Prepayments, Payment Holidays, Interest Rate Changes)
- **RBI compliance** with complete audit trail
- **High scalability** (300 TPS, <5s latency, 99.99% availability)

## 📋 Table of Contents

- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Getting Started](#getting-started)
- [API Documentation](#api-documentation)
- [Configuration](#configuration)
- [Deployment](#deployment)
- [Testing](#testing)
- [Monitoring](#monitoring)
- [Contributing](#contributing)

## 🏗️ Architecture

### High-Level Architecture

```
┌─────────────────┐         ┌──────────────────┐         ┌─────────────────┐
│  .NET Monolith  │────────▶│  API Gateway     │────────▶│  Amortisation   │
│  (On-Premise)   │  OAuth2 │  (Kong/AWS)      │         │  Microservice   │
└─────────────────┘         └──────────────────┘         │  (Java/EKS)     │
                                                          └────────┬────────┘
                                                                   │
                                    ┌──────────────────────────────┴───────┐
                                    │                                      │
                            ┌───────▼─────────┐                   ┌───────▼────────┐
                            │  Redis Cluster  │                   │  Oracle DB     │
                            │  (Caching)      │                   │  (On-Premise)  │
                            └─────────────────┘                   └────────────────┘
```

### Component Architecture

- **API Layer**: REST controllers with OAuth2 security
- **Service Layer**: Business logic and calculation engine
- **Data Access Layer**: JPA repositories with connection pooling
- **Caching Layer**: Redis for product configs and calculation results
- **Batch Layer**: Spring Batch for month-end processing

## 🛠️ Technology Stack

| Category | Technology |
|----------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.2 |
| Database | Oracle 19c |
| Cache | Redis 7.x |
| Container | Docker |
| Orchestration | Kubernetes (EKS) |
| Security | OAuth2 + Spring Security |
| Monitoring | Prometheus, Grafana, CloudWatch, DataDog |
| Build | Maven 3.9 |
| CI/CD | GitHub Actions |

## 🚀 Getting Started

### Prerequisites

- Java 21 or higher
- Maven 3.9+
- Docker & Docker Compose
- Oracle Database 19c+ (or use Docker)
- Redis 7.x (or use Docker)

### Local Development Setup

1. **Clone the repository**
```bash
git clone https://github.com/your-org/amortisation-microservice.git
cd amortisation-microservice
```

2. **Start infrastructure (Oracle + Redis)**
```bash
docker-compose up -d
```

3. **Configure application properties**
```bash
cp src/main/resources/application.yml src/main/resources/application-local.yml
# Edit application-local.yml with your local settings
```

4. **Build the application**
```bash
mvn clean install
```

5. **Run the application**
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

6. **Access the application**
- API: http://localhost:8080/api/v1/amortisation
- Swagger UI: http://localhost:8080/swagger-ui.html
- Actuator: http://localhost:8080/actuator

## 📚 API Documentation

### Calculate EMI Schedule

**Endpoint**: `POST /api/v1/amortisation/calculate`

**Request**:
```json
{
  "loanId": "LN123456789",
  "principal": 5000000.00,
  "interestRate": 8.5,
  "tenure": 240,
  "productType": "HOME_LOAN",
  "amortisationMethod": "REDUCING_BALANCE",
  "startDate": "2025-01-01",
  "frequency": "MONTHLY"
}
```

**Response**:
```json
{
  "loanId": "LN123456789",
  "emi": 43391.00,
  "totalInterest": 5413840.00,
  "totalPayment": 10413840.00,
  "schedule": [
    {
      "installmentNumber": 1,
      "dueDate": "2025-02-01",
      "openingBalance": 5000000.00,
      "emi": 43391.00,
      "principal": 8058.00,
      "interest": 35333.00,
      "closingBalance": 4991942.00
    }
    // ... more installments
  ],
  "auditTrail": "Amortisation Method: REDUCING_BALANCE | Formula: EMI = P × r × (1+r)^n / ((1+r)^n - 1) | ..."
}
```

### Get Loan Schedule

**Endpoint**: `GET /api/v1/amortisation/schedule/{loanId}`

### Submit Batch Job

**Endpoint**: `POST /api/v1/amortisation/batch/submit`

For complete API documentation, visit: http://localhost:8080/swagger-ui.html

## ⚙️ Configuration

### Application Configuration

Key configuration properties in `application.yml`:

```yaml
app:
  amortisation:
    calculation:
      decimal-precision: 15
      max-tenure-months: 360

    cache:
      product-config-ttl: 3600
      calculation-result-ttl: 900

    batch:
      chunk-size: 1000
      parallel-threads: 10

    compliance:
      audit-enabled: true
      regulatory-version: "RBI-2024-v1"
```

### Environment Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `ORACLE_HOST` | Oracle database host | `db.example.com` |
| `ORACLE_PORT` | Oracle database port | `1521` |
| `ORACLE_USERNAME` | Database username | `lms_user` |
| `ORACLE_PASSWORD` | Database password | `changeme` |
| `REDIS_HOST` | Redis host | `redis.example.com` |
| `REDIS_PASSWORD` | Redis password | `changeme` |
| `OAUTH2_ISSUER_URI` | OAuth2 issuer | `https://auth.example.com` |

## 🐳 Deployment

### Docker Build

```bash
# Using Jib (no Docker daemon required)
mvn compile jib:build

# Or using Docker
docker build -t amortisation-microservice:latest .
```

### Kubernetes Deployment

```bash
# Create namespace
kubectl create namespace lms

# Apply configurations
kubectl apply -f infrastructure/kubernetes/configmap.yml
kubectl apply -f infrastructure/kubernetes/secrets.yml
kubectl apply -f infrastructure/kubernetes/deployment.yml
kubectl apply -f infrastructure/kubernetes/service.yml
kubectl apply -f infrastructure/kubernetes/hpa.yml
kubectl apply -f infrastructure/kubernetes/ingress.yml

# Check deployment status
kubectl get pods -n lms
kubectl logs -f deployment/amortisation-service -n lms
```

### Helm Deployment

```bash
# Install using Helm
helm install amortisation-service ./infrastructure/helm/amortisation-service \
  --namespace lms \
  --values ./infrastructure/helm/amortisation-service/values-prod.yaml
```

## 🧪 Testing

### Unit Tests

```bash
mvn test
```

### Integration Tests

```bash
mvn verify -P integration-tests
```

### Load Testing

```bash
# Using JMeter
jmeter -n -t tests/load/amortisation-load-test.jmx -l results.jtl

# Using Gatling
mvn gatling:test
```

### Test Coverage

```bash
mvn clean test jacoco:report
# Report available at: target/site/jacoco/index.html
```

## 📊 Monitoring

### Metrics

The service exposes Prometheus metrics at `/actuator/prometheus`:

- `amortisation_calculation_duration_seconds` - Time taken for calculations
- `amortisation_calculation_total` - Total number of calculations
- `amortisation_calculation_errors_total` - Total calculation errors
- `amortisation_cache_hit_ratio` - Cache hit rate

### Dashboards

Pre-configured Grafana dashboards are available in `monitoring/grafana/dashboards/`:

1. **Business Metrics Dashboard**
   - Calculations per hour
   - Product type distribution
   - Average EMI calculated

2. **Technical Metrics Dashboard**
   - Request rate (TPS)
   - P95/P99 latency
   - Error rate
   - Pod count and resources

3. **Compliance Dashboard**
   - Audit logs written
   - Calculation verification rate

### Alerts

Prometheus alerts configured in `monitoring/prometheus/alerts.yml`:

- High error rate (>1%)
- High latency (P95 >5s)
- High CPU/Memory usage
- Oracle connection pool exhaustion

## 🔒 Security

### Authentication & Authorization

- **OAuth2** for API authentication
- **JWT tokens** with scope-based authorization
- **Scopes**:
  - `amortisation:calculate` - For calculation APIs
  - `amortisation:batch` - For batch operations
  - `amortisation:admin` - For admin operations

### Data Security

- TLS 1.3 for all communications
- VPN/Direct Connect for on-premise connectivity
- Secrets stored in AWS Secrets Manager
- Database credentials rotated every 90 days

## 📋 Project Structure

```
amortisation-microservice/
├── src/
│   ├── main/
│   │   ├── java/com/lms/amortisation/
│   │   │   ├── AmortisationApplication.java
│   │   │   ├── config/              # Spring configuration
│   │   │   ├── controller/          # REST controllers
│   │   │   ├── service/             # Business logic
│   │   │   │   ├── calculator/      # Calculation engines
│   │   │   │   ├── edgecase/        # Edge case handlers
│   │   │   │   ├── batch/           # Batch processing
│   │   │   │   └── audit/           # Compliance & audit
│   │   │   ├── repository/          # Data access
│   │   │   ├── model/               # Domain models & DTOs
│   │   │   ├── exception/           # Exception handling
│   │   │   └── util/                # Utilities
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/changelog/        # Database migrations
│   └── test/                        # Tests
├── infrastructure/
│   ├── kubernetes/                  # K8s manifests
│   ├── helm/                        # Helm charts
│   ├── terraform/                   # Infrastructure as Code
│   └── docker/                      # Docker configs
├── monitoring/                      # Monitoring configs
├── docs/                            # Documentation
├── pom.xml
└── README.md
```

## 🎯 Performance Targets

| Metric | Target | Current |
|--------|--------|---------|
| P95 Latency | <5 seconds | TBD |
| Throughput | 300 TPS | TBD |
| Availability | 99.99% | TBD |
| Error Rate | <1% | TBD |
| Cache Hit Rate | >80% | TBD |

## 🛣️ Roadmap

### Phase 1: Foundation (Months 1-3) ✅
- [x] Architecture design
- [ ] Core calculation engine
- [ ] REST API implementation
- [ ] Oracle integration
- [ ] Redis caching
- [ ] Audit & compliance
- [ ] Shadow mode testing
- [ ] 10% production traffic

### Phase 2: Optimization (Months 4-6)
- [ ] Batch processing
- [ ] Performance optimization
- [ ] 100% traffic migration
- [ ] Legacy decommission

### Phase 3: Event-Driven (Months 7-9)
- [ ] CDC pipeline
- [ ] PostgreSQL migration
- [ ] Event-driven processing

## 📖 Documentation

Detailed documentation available in the `docs/` directory:

- [Architecture Design](ARCHITECTURE_DESIGN.md)
- [Implementation Plan](IMPLEMENTATION_PLAN.md)
- [API Documentation](docs/API.md)
- [Runbook](docs/RUNBOOK.md)
- [Compliance Guide](docs/COMPLIANCE.md)

## 👥 Team

- **Tech Lead**: TBD
- **Senior Java Developers**: TBD (2)
- **Java Developers**: TBD (2)
- **DevOps Engineer**: TBD
- **QA Engineers**: TBD (2)

## 📞 Support

For issues and questions:
- **Jira**: https://jira.example.com/LMS-AMORT
- **Slack**: #amortisation-microservice
- **Email**: lms-team@example.com

## 📄 License

Internal project - Proprietary

---

**Generated with Claude Code** 🤖

Last Updated: 2025-11-02
