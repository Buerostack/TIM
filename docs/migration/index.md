---
layout: page
title: KeyCloak Migration Guide
permalink: /migration/
---

# Migrating from KeyCloak to TIM 2.0

## Why Migrate to TIM 2.0?

TIM 2.0 offers significant advantages over KeyCloak for JWT-focused use cases:

### Complexity Reduction
- **KeyCloak**: Multiple services, complex configuration, heavy resource usage
- **TIM 2.0**: Single container, minimal configuration, lightweight

### Advanced JWT Features
- **KeyCloak**: Basic JWT generation and validation
- **TIM 2.0**: Extension chains, complete audit trails, INSERT-only architecture

### Operational Benefits
- **KeyCloak**: Complex deployment, requires expertise
- **TIM 2.0**: Docker-first, developer-friendly, minimal maintenance

## Migration Assessment

### Suitable Use Cases for Migration
✅ **Perfect for TIM 2.0:**
- JWT-based authentication/authorization
- API-first applications
- Microservices requiring token introspection
- Systems needing audit trails
- Docker/container deployments
- Simple user management needs

❌ **Stick with KeyCloak:**
- Complex user federation requirements
- Heavy LDAP/Active Directory integration
- Advanced social login flows
- Complex realm/group hierarchies
- Legacy application integration

## Feature Comparison

| Feature | KeyCloak | TIM 2.0 | Migration Notes |
|---------|----------|---------|-----------------|
| **JWT Generation** | ✅ Standard | ✅ Advanced + Extension Chains | Direct migration possible |
| **Token Introspection** | ✅ RFC 7662 | ✅ RFC 7662 + Metadata | Enhanced introspection |
| **Audit Trails** | ⚠️ Limited | ✅ Complete Immutable | Significant improvement |
| **User Management** | ✅ Full Admin UI | ⚠️ API-based | Requires UI development |
| **Social Login** | ✅ Extensive | ❌ Not supported | External solution needed |
| **LDAP Integration** | ✅ Full Support | ❌ Not supported | Custom integration required |
| **Resource Usage** | ❌ Heavy | ✅ Lightweight | 10x reduction typical |
| **Setup Complexity** | ❌ High | ✅ Minimal | Hours vs minutes |

## Step-by-Step Migration

### Phase 1: Assessment and Planning

#### 1. Analyze Current KeyCloak Usage
```bash
# Export KeyCloak configuration
/opt/keycloak/bin/kc.sh export --file keycloak-export.json

# Analyze realms, clients, and users
jq '.realms[] | {name: .realm, clients: [.clients[].clientId]}' keycloak-export.json
```

#### 2. Identify Migration Scope
- List all applications using KeyCloak
- Document JWT validation endpoints
- Identify custom claims and scopes
- Map user roles and permissions

#### 3. Plan Migration Strategy
- **Big Bang**: Switch all at once (recommended for simple setups)
- **Gradual**: Migrate application by application
- **Parallel**: Run both systems during transition

### Phase 2: TIM 2.0 Setup

#### 1. Deploy TIM 2.0
```bash
# Clone TIM 2.0
git clone https://github.com/buerostack/TIM.git
cd TIM

# Configure environment
cp .env.example .env
# Edit .env with your settings

# Deploy
docker-compose up -d
```

#### 2. Configure JWT Signing
```bash
# TIM 2.0 generates RSA keys automatically
# Or provide your own in application.properties:
# jwt.signer.private-key-path=/path/to/private.pem
# jwt.signer.public-key-path=/path/to/public.pem
```

#### 3. Test Basic Functionality
```bash
# Test JWT generation
curl -X POST http://localhost:8080/jwt/custom/generate \
  -H "Content-Type: application/json" \
  -d '{
    "JWTName": "TEST_TOKEN",
    "content": {"sub": "testuser", "role": "admin"},
    "expirationInMinutes": 60
  }'
```

### Phase 3: Application Migration

#### 1. Update JWT Validation Endpoints
**Before (KeyCloak):**
```javascript
// KeyCloak token introspection
const response = await fetch('http://keycloak:8080/auth/realms/myrealm/protocol/openid_connect/token/introspect', {
  method: 'POST',
  headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
  body: `token=${token}&client_id=myclient&client_secret=secret`
});
```

**After (TIM 2.0):**
```javascript
// TIM 2.0 token introspection
const response = await fetch('http://tim:8080/introspect', {
  method: 'POST',
  headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
  body: `token=${token}`
});
```

#### 2. Update JWT Generation
**Before (KeyCloak):**
```bash
curl -X POST http://keycloak:8080/auth/realms/myrealm/protocol/openid_connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials&client_id=myclient&client_secret=secret"
```

**After (TIM 2.0):**
```bash
curl -X POST http://tim:8080/jwt/custom/generate \
  -H "Content-Type: application/json" \
  -d '{
    "JWTName": "API_TOKEN",
    "content": {"sub": "service", "scope": "api:read"},
    "expirationInMinutes": 60
  }'
```

#### 3. Leverage Extension Chains
```javascript
// Extend token lifetime (new capability)
const extendResponse = await fetch('http://tim:8080/jwt/custom/extend', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    token: currentToken,
    expirationInMinutes: 120
  })
});

// Get complete audit trail (new capability)
const chainResponse = await fetch(`http://tim:8080/jwt/custom/extension-chain/${originalJwtUuid}`);
const auditTrail = await chainResponse.json();
```

### Phase 4: Data Migration

#### 1. User Data Migration
TIM 2.0 focuses on tokens, not user management. Options:

**Option A: External User Service**
```javascript
// Implement user management separately
const userService = new UserService(database);
const user = await userService.authenticate(username, password);

if (user) {
  const tokenResponse = await fetch('http://tim:8080/jwt/custom/generate', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      JWTName: 'USER_SESSION',
      content: {
        sub: user.id,
        email: user.email,
        roles: user.roles
      },
      expirationInMinutes: 480
    })
  });
}
```

**Option B: Migrate to External Auth Provider**
```javascript
// Use Auth0, Firebase Auth, or similar
const auth0Token = await auth0.getAccessToken();
// Convert to TIM 2.0 format for internal use
```

#### 2. Role and Permission Mapping
```javascript
// KeyCloak roles → TIM 2.0 claims
const keycloakToTIMMapping = {
  'realm:admin': { role: 'admin', scope: 'all' },
  'client:user': { role: 'user', scope: 'read' },
  'client:api': { role: 'service', scope: 'api' }
};

function mapKeycloakRoles(keycloakRoles) {
  return keycloakRoles.reduce((claims, role) => {
    const mapping = keycloakToTIMMapping[role];
    return { ...claims, ...mapping };
  }, {});
}
```

### Phase 5: Testing and Validation

#### 1. Integration Testing
```bash
# Test all application endpoints with TIM 2.0 tokens
./run-integration-tests.sh --token-provider=tim

# Validate token introspection responses
./validate-introspection.sh
```

#### 2. Performance Testing
```bash
# Compare performance: KeyCloak vs TIM 2.0
wrk -t12 -c400 -d30s --script=auth-test.lua http://keycloak:8080/auth/realms/myrealm/protocol/openid_connect/token/introspect
wrk -t12 -c400 -d30s --script=auth-test.lua http://tim:8080/introspect
```

#### 3. Security Validation
- Verify JWT signature validation
- Test token revocation (denylist)
- Validate extension chain security
- Test rate limiting

### Phase 6: Deployment and Cutover

#### 1. Blue-Green Deployment
```yaml
# docker-compose.yml
version: '3.8'
services:
  tim-blue:
    image: tim:2.0
    environment:
      - ENVIRONMENT=blue

  tim-green:
    image: tim:2.0
    environment:
      - ENVIRONMENT=green

  nginx:
    image: nginx
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
```

#### 2. DNS/Load Balancer Switch
```bash
# Switch traffic from KeyCloak to TIM 2.0
kubectl patch service auth-service -p '{"spec":{"selector":{"app":"tim"}}}'
```

#### 3. Monitor and Validate
```bash
# Monitor key metrics
curl http://tim:8080/actuator/metrics/jwt.generation.count
curl http://tim:8080/actuator/metrics/jwt.introspection.count
curl http://tim:8080/actuator/health
```

## Migration Checklist

### Pre-Migration
- [ ] Document current KeyCloak configuration
- [ ] Identify all applications using KeyCloak
- [ ] Plan user management strategy
- [ ] Set up TIM 2.0 test environment
- [ ] Create migration scripts
- [ ] Test token validation in all applications

### Migration Day
- [ ] Deploy TIM 2.0 to production
- [ ] Update DNS/load balancer configuration
- [ ] Switch application configurations
- [ ] Verify all endpoints are responding
- [ ] Monitor error rates and performance
- [ ] Execute rollback plan if needed

### Post-Migration
- [ ] Monitor system health for 48 hours
- [ ] Validate audit trails are working
- [ ] Test token extension functionality
- [ ] Update documentation
- [ ] Train team on TIM 2.0 operations
- [ ] Decommission KeyCloak (after grace period)

## Rollback Plan

### Immediate Rollback (< 1 hour)
```bash
# Switch load balancer back to KeyCloak
kubectl patch service auth-service -p '{"spec":{"selector":{"app":"keycloak"}}}'

# Revert application configurations
./revert-to-keycloak.sh
```

### Data Considerations
- TIM 2.0 tokens are not compatible with KeyCloak
- Users may need to re-authenticate
- Active sessions may be lost

## Common Migration Issues

### Issue: JWT Format Differences
**Problem**: Applications expect specific KeyCloak JWT claims
**Solution**: Customize TIM 2.0 JWT generation to match expected format

### Issue: User Management
**Problem**: Applications rely on KeyCloak user APIs
**Solution**: Implement external user service or migrate to dedicated auth provider

### Issue: Performance Tuning
**Problem**: Different performance characteristics
**Solution**: Adjust database connection pools and caching configuration

## Support and Resources

- **Documentation**: [TIM 2.0 Docs](https://buerostack.github.io/TIM)
- **GitHub Issues**: [Report Migration Issues](https://github.com/buerostack/TIM/issues)
- **Community**: [Discussions](https://github.com/buerostack/TIM/discussions)

## Success Metrics

Track these metrics to measure migration success:

- **Response Time**: Should improve significantly
- **Resource Usage**: CPU/Memory should decrease
- **Deployment Time**: Should be much faster
- **Operational Complexity**: Fewer moving parts
- **Developer Experience**: Simpler API, better documentation