# How to Set Up Development Environment

This guide walks you through setting up a complete development environment for TIM 2.0.

## Prerequisites

Before you begin, ensure you have the following installed:

- **Java Development Kit (JDK) 17 or higher**
  - Download from [Oracle](https://www.oracle.com/java/technologies/downloads/) or use [OpenJDK](https://openjdk.org/)
  - Verify installation: `java -version`

- **Apache Maven 3.8 or higher**
  - Download from [Maven Apache](https://maven.apache.org/download.cgi)
  - Verify installation: `mvn -version`

- **Docker Desktop** (recommended) or Docker Engine + Docker Compose
  - Download from [Docker](https://www.docker.com/products/docker-desktop/)
  - Verify installation: `docker --version` and `docker-compose --version`

- **Git**
  - Download from [Git SCM](https://git-scm.com/)
  - Verify installation: `git --version`

- **IDE** (optional but recommended)
  - [IntelliJ IDEA](https://www.jetbrains.com/idea/) (Community or Ultimate)
  - [Visual Studio Code](https://code.visualstudio.com/) with Java extensions
  - [Eclipse IDE for Java Developers](https://www.eclipse.org/downloads/)

## Step 1: Clone the Repository

```bash
git clone https://github.com/Buerostack/TIM.git
cd TIM
```

## Step 2: Start PostgreSQL Database

TIM 2.0 requires PostgreSQL for data persistence. The easiest way is to use Docker Compose:

```bash
# Start only PostgreSQL
docker-compose up -d postgres

# Verify PostgreSQL is running
docker-compose ps
```

The database will be initialized with the schema automatically on first startup.

### Database Connection Details
- **Host**: localhost
- **Port**: 5432
- **Database**: tim
- **Username**: tim
- **Password**: tim

## Step 3: Build the Project

```bash
# From the TIM root directory
mvn clean install

# This will:
# 1. Download all dependencies
# 2. Compile all modules
# 3. Run tests
# 4. Package the application
```

## Step 4: Run the Application

### Option A: Using Maven (Recommended for Development)

```bash
cd app/server
mvn spring-boot:run
```

### Option B: Using the JAR file

```bash
# After building with mvn clean install
java -jar app/server/target/tim-server-2.0.0.jar
```

### Option C: Using Docker Compose (Full Stack)

```bash
# From TIM root directory
docker-compose up -d

# View logs
docker-compose logs -f tim
```

## Step 5: Verify the Installation

Once the application starts, verify it's working:

### Check Application Health
```bash
curl http://localhost:8085/auth/health
```

Expected response:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP"
    }
  }
}
```

### Access Swagger UI
Open your browser and navigate to:
```
http://localhost:8085
```

You should see the interactive API documentation.

### Test Token Generation
```bash
curl -X POST http://localhost:8085/jwt/custom/generate \
  -H "Content-Type: application/json" \
  -d '{
    "JWTName": "dev-test-token",
    "content": {
      "sub": "developer",
      "role": "admin"
    },
    "expirationInMinutes": 60
  }'
```

## Step 6: IDE Configuration

### IntelliJ IDEA

1. **Import Project**:
   - File → Open → Select the `TIM` directory
   - IntelliJ will automatically detect the Maven project

2. **Configure JDK**:
   - File → Project Structure → Project
   - Set Project SDK to Java 17+

3. **Enable Annotation Processing** (for Lombok, if used):
   - Settings → Build, Execution, Deployment → Compiler → Annotation Processors
   - Check "Enable annotation processing"

4. **Run Configuration**:
   - Create new Spring Boot run configuration
   - Main class: `com.buerostack.tim.TimApplication`
   - Module: `tim-server`

### Visual Studio Code

1. **Install Extensions**:
   - Extension Pack for Java
   - Spring Boot Extension Pack
   - Lombok Annotations Support

2. **Open Project**:
   - File → Open Folder → Select `TIM` directory

3. **Configure Java**:
   - Settings → Java: Home → Set to JDK 17+ path

## Environment Variables

Create a `.env.local` file in the project root for local development:

```bash
# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/tim
DATABASE_USERNAME=tim
DATABASE_PASSWORD=tim

# JWT Signing Key Password
KEY_PASS=development-key-password

# OAuth2 Providers (optional)
OAUTH2_PROVIDERS={}

# Server Port (optional, defaults to 8085)
SERVER_PORT=8085

# Log Level (optional)
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_COM_BUEROSTACK_TIM=DEBUG
```

## Common Development Tasks

### Run Tests
```bash
# All tests
mvn test

# Specific module
cd app/custom-jwt
mvn test

# With coverage
mvn test jacoco:report
```

### Clean Build
```bash
mvn clean install
```

### Skip Tests (faster builds)
```bash
mvn clean install -DskipTests
```

### Format Code
```bash
mvn spotless:apply
```

### Database Migrations
```bash
# If using Flyway
mvn flyway:migrate

# Reset database (development only!)
mvn flyway:clean flyway:migrate
```

## Troubleshooting

### Port Already in Use
If port 8085 is already in use:

```bash
# Change port in application.yml or via environment variable
SERVER_PORT=8086 mvn spring-boot:run
```

### Database Connection Issues
```bash
# Check if PostgreSQL is running
docker-compose ps postgres

# View PostgreSQL logs
docker-compose logs postgres

# Restart PostgreSQL
docker-compose restart postgres
```

### Build Failures
```bash
# Clean Maven cache
mvn dependency:purge-local-repository

# Clean and rebuild
mvn clean install -U
```

### Out of Memory
```bash
# Increase Maven memory
export MAVEN_OPTS="-Xmx2048m -XX:MaxPermSize=512m"
mvn clean install
```

## Next Steps

- **Configure OAuth2 Providers**: See [configure-oauth2-provider.md](configure-oauth2-provider.md)
- **Generate Custom JWTs**: See [generate-custom-jwt.md](generate-custom-jwt.md)
- **Explore API**: Open Swagger UI at http://localhost:8085
- **Run Examples**: Check the `examples/` directory for integration examples

## Development Workflow

1. **Create a feature branch**: `git checkout -b feature/your-feature`
2. **Make changes and test locally**: `mvn test`
3. **Run the application**: `mvn spring-boot:run`
4. **Test via Swagger UI**: http://localhost:8085
5. **Commit and push**: Follow guidelines in [CONTRIBUTING.md](../../CONTRIBUTING.md)
6. **Create pull request**: Submit for review

## Useful Commands Reference

```bash
# Start everything
docker-compose up -d

# Stop everything
docker-compose down

# View logs
docker-compose logs -f tim

# Rebuild after changes
mvn clean install && docker-compose up -d --build

# Access PostgreSQL CLI
docker-compose exec postgres psql -U tim -d tim

# Run specific test class
mvn test -Dtest=CustomJwtControllerTest

# Package for production
mvn clean package -Pprod
```
