---
layout: page
title: Deployment Guide
permalink: /deployment/
---

# TIM 2.0 Deployment Guide

## Quick Start

### Prerequisites
- Docker and Docker Compose
- Git
- 2GB RAM minimum, 4GB recommended
- PostgreSQL (included in Docker setup)

### Local Development
```bash
# Clone repository
git clone https://github.com/buerostack/TIM.git
cd TIM

# Start services
docker-compose up -d

# Verify deployment
curl http://localhost:8080/actuator/health
```

TIM 2.0 will be available at `http://localhost:8080`

## Production Deployment

### Docker Compose (Recommended)

#### 1. Production Configuration
```yaml
# docker-compose.prod.yml
version: '3.8'
services:
  tim:
    image: buerostack/tim:2.0
    restart: unless-stopped
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DATABASE_URL=jdbc:postgresql://postgres:5432/tim
      - DATABASE_USERNAME=tim_user
      - DATABASE_PASSWORD=${DB_PASSWORD}
      - JWT_ISSUER=https://your-domain.com
      - JWT_AUDIENCE=your-api-audience
    depends_on:
      - postgres
    volumes:
      - ./logs:/app/logs
      - ./config:/app/config

  postgres:
    image: postgres:15
    restart: unless-stopped
    environment:
      - POSTGRES_DB=tim
      - POSTGRES_USER=tim_user
      - POSTGRES_PASSWORD=${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./db/init.sql:/docker-entrypoint-initdb.d/init.sql
    ports:
      - "5432:5432"

  nginx:
    image: nginx:alpine
    restart: unless-stopped
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
      - ./ssl:/etc/nginx/ssl
    depends_on:
      - tim

volumes:
  postgres_data:
```

#### 2. Environment Configuration
```bash
# .env file
DB_PASSWORD=your-strong-password
JWT_ISSUER=https://your-domain.com
JWT_AUDIENCE=your-api-audience
ENVIRONMENT=production
```

#### 3. NGINX Configuration
```nginx
# nginx.conf
events {
    worker_connections 1024;
}

http {
    upstream tim_backend {
        server tim:8080;
    }

    server {
        listen 80;
        server_name your-domain.com;
        return 301 https://$server_name$request_uri;
    }

    server {
        listen 443 ssl http2;
        server_name your-domain.com;

        ssl_certificate /etc/nginx/ssl/cert.pem;
        ssl_certificate_key /etc/nginx/ssl/key.pem;

        location / {
            proxy_pass http://tim_backend;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        # Health check endpoint
        location /health {
            proxy_pass http://tim_backend/actuator/health;
        }
    }
}
```

### Kubernetes Deployment

#### 1. Namespace and ConfigMap
```yaml
# k8s/namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: tim

---
# k8s/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: tim-config
  namespace: tim
data:
  application.properties: |
    spring.profiles.active=prod
    server.port=8080
    jwt.issuer=https://your-domain.com
    jwt.audience=your-api-audience
```

#### 2. PostgreSQL Deployment
```yaml
# k8s/postgres.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: postgres
  namespace: tim
spec:
  replicas: 1
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
    spec:
      containers:
      - name: postgres
        image: postgres:15
        env:
        - name: POSTGRES_DB
          value: tim
        - name: POSTGRES_USER
          value: tim_user
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: tim-secrets
              key: db-password
        ports:
        - containerPort: 5432
        volumeMounts:
        - name: postgres-storage
          mountPath: /var/lib/postgresql/data
      volumes:
      - name: postgres-storage
        persistentVolumeClaim:
          claimName: postgres-pvc

---
apiVersion: v1
kind: Service
metadata:
  name: postgres
  namespace: tim
spec:
  selector:
    app: postgres
  ports:
  - port: 5432
    targetPort: 5432
```

#### 3. TIM 2.0 Deployment
```yaml
# k8s/tim.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: tim
  namespace: tim
spec:
  replicas: 3
  selector:
    matchLabels:
      app: tim
  template:
    metadata:
      labels:
        app: tim
    spec:
      containers:
      - name: tim
        image: buerostack/tim:2.0
        ports:
        - containerPort: 8080
        env:
        - name: DATABASE_URL
          value: jdbc:postgresql://postgres:5432/tim
        - name: DATABASE_USERNAME
          value: tim_user
        - name: DATABASE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: tim-secrets
              key: db-password
        volumeMounts:
        - name: config
          mountPath: /app/config
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
      volumes:
      - name: config
        configMap:
          name: tim-config

---
apiVersion: v1
kind: Service
metadata:
  name: tim-service
  namespace: tim
spec:
  selector:
    app: tim
  ports:
  - port: 80
    targetPort: 8080
  type: LoadBalancer
```

### Cloud Deployments

#### AWS ECS with Fargate
```json
{
  "family": "tim-task",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "512",
  "memory": "1024",
  "executionRoleArn": "arn:aws:iam::123456789012:role/ecsTaskExecutionRole",
  "containerDefinitions": [
    {
      "name": "tim",
      "image": "buerostack/tim:2.0",
      "portMappings": [
        {
          "containerPort": 8080,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {
          "name": "SPRING_PROFILES_ACTIVE",
          "value": "prod"
        },
        {
          "name": "DATABASE_URL",
          "value": "jdbc:postgresql://your-rds-endpoint:5432/tim"
        }
      ],
      "secrets": [
        {
          "name": "DATABASE_PASSWORD",
          "valueFrom": "arn:aws:secretsmanager:region:account:secret:tim-db-password"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/tim",
          "awslogs-region": "us-east-1",
          "awslogs-stream-prefix": "ecs"
        }
      }
    }
  ]
}
```

#### Google Cloud Run
```yaml
# cloud-run.yaml
apiVersion: serving.knative.dev/v1
kind: Service
metadata:
  name: tim
  annotations:
    run.googleapis.com/ingress: all
spec:
  template:
    metadata:
      annotations:
        run.googleapis.com/cpu-throttling: "false"
    spec:
      containerConcurrency: 100
      containers:
      - image: gcr.io/your-project/tim:2.0
        ports:
        - containerPort: 8080
        env:
        - name: DATABASE_URL
          value: jdbc:postgresql://sql-proxy:5432/tim
        - name: DATABASE_PASSWORD
          valueFrom:
            secretKeyRef:
              key: password
              name: db-secret
        resources:
          limits:
            cpu: "2"
            memory: "2Gi"
```

## Configuration

### Application Properties

#### Production Configuration
```properties
# application-prod.properties
server.port=8080
server.servlet.context-path=/

# Database
spring.datasource.url=${DATABASE_URL}
spring.datasource.username=${DATABASE_USERNAME}
spring.datasource.password=${DATABASE_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=false

# JWT Configuration
jwt.issuer=${JWT_ISSUER:TIM}
jwt.audience=${JWT_AUDIENCE:tim-audience}
jwt.default-expiration-minutes=60

# Security
server.ssl.enabled=false
management.endpoints.web.exposure.include=health,info,metrics

# Logging
logging.level.root=INFO
logging.level.buerostack=INFO
logging.file.name=/app/logs/tim.log
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %msg%n
```

#### Security Configuration
```properties
# application-secure.properties
# HTTPS Configuration
server.ssl.enabled=true
server.ssl.key-store=/app/config/keystore.p12
server.ssl.key-store-password=${KEYSTORE_PASSWORD}
server.ssl.key-store-type=PKCS12

# Rate Limiting
rate-limit.jwt-generation=100
rate-limit.introspection=1000
rate-limit.window-minutes=1

# Database Connection Pool
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
```

### Environment Variables

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `dev` | No |
| `DATABASE_URL` | PostgreSQL connection URL | `jdbc:postgresql://localhost:5432/tim` | Yes |
| `DATABASE_USERNAME` | Database username | `tim` | Yes |
| `DATABASE_PASSWORD` | Database password | - | Yes |
| `JWT_ISSUER` | JWT issuer claim | `TIM` | No |
| `JWT_AUDIENCE` | JWT audience claim | `tim-audience` | No |
| `SERVER_PORT` | Application port | `8080` | No |

## Monitoring and Observability

### Health Checks
```bash
# Application health
curl http://localhost:8080/actuator/health

# Database connectivity
curl http://localhost:8080/actuator/health/db

# Disk space
curl http://localhost:8080/actuator/health/diskSpace
```

### Metrics
```bash
# JWT generation metrics
curl http://localhost:8080/actuator/metrics/jwt.generation.count

# Introspection metrics
curl http://localhost:8080/actuator/metrics/jwt.introspection.count

# Database connection pool
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active
```

### Logging Configuration
```xml
<!-- logback-spring.xml -->
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>

    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>/app/logs/tim.log</file>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>/app/logs/tim.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
    </appender>

    <root level="INFO">
        <appender-ref ref="STDOUT"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

## Backup and Recovery

### Database Backup
```bash
# Create backup
docker exec postgres pg_dump -U tim_user tim > tim_backup_$(date +%Y%m%d).sql

# Automated backup script
#!/bin/bash
BACKUP_DIR="/backups"
DATE=$(date +%Y%m%d_%H%M%S)
docker exec postgres pg_dump -U tim_user tim | gzip > $BACKUP_DIR/tim_$DATE.sql.gz

# Keep only last 30 days
find $BACKUP_DIR -name "tim_*.sql.gz" -mtime +30 -delete
```

### Restore Database
```bash
# Stop TIM service
docker-compose stop tim

# Restore database
gunzip -c tim_20240115_120000.sql.gz | docker exec -i postgres psql -U tim_user -d tim

# Start TIM service
docker-compose start tim
```

### Disaster Recovery
```bash
# Full system backup
tar -czf tim_system_backup_$(date +%Y%m%d).tar.gz \
  docker-compose.yml \
  .env \
  nginx.conf \
  logs/ \
  config/

# Recovery procedure
# 1. Restore Docker Compose configuration
# 2. Restore database from backup
# 3. Start services
# 4. Verify functionality
```

## Security Considerations

### Network Security
- Use HTTPS in production
- Implement proper firewall rules
- Consider API gateway for additional security
- Use VPN for database access

### Application Security
- Regular security updates
- Secure database credentials
- Monitor for suspicious activity
- Implement rate limiting

### Compliance
- Enable audit logging
- Regular security assessments
- Data retention policies
- Access control reviews

## Troubleshooting

### Common Issues

#### Database Connection Issues
```bash
# Check database connectivity
docker exec tim curl -f http://localhost:8080/actuator/health/db

# Check PostgreSQL logs
docker logs postgres

# Verify database schema
docker exec postgres psql -U tim_user -d tim -c "\dt custom_jwt.*"
```

#### Performance Issues
```bash
# Check JVM memory usage
docker exec tim curl http://localhost:8080/actuator/metrics/jvm.memory.used

# Check database query performance
docker exec postgres psql -U tim_user -d tim -c "SELECT * FROM pg_stat_activity;"

# Monitor response times
curl -w "@curl-format.txt" http://localhost:8080/actuator/health
```

#### SSL/TLS Issues
```bash
# Test SSL configuration
openssl s_client -connect your-domain.com:443

# Check certificate validity
curl -vI https://your-domain.com/actuator/health
```

## Performance Tuning

### JVM Optimization
```bash
# Docker environment variables for JVM tuning
JAVA_OPTS: "-Xmx1g -Xms512m -XX:+UseG1GC -XX:+UseStringDeduplication"
```

### Database Optimization
```sql
-- Monitor query performance
SELECT query, mean_time, calls FROM pg_stat_statements ORDER BY mean_time DESC LIMIT 10;

-- Optimize connection pool
ALTER SYSTEM SET max_connections = 200;
ALTER SYSTEM SET shared_buffers = '256MB';
```

### Caching Configuration
```properties
# Redis caching (optional)
spring.cache.type=redis
spring.redis.host=redis
spring.redis.port=6379
spring.redis.timeout=2000ms
```