# Troubleshooting DB Issues

## App boots but fails at queries
- Check that `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` are correct in the running container:
  ```bash
  docker exec tim printenv | grep SPRING_DATASOURCE
  ```
- Verify schemas exist and tables are created:
  ```bash
  docker exec -it tim-postgres psql -U tim -d tim -c '\dn'
  docker exec -it tim-postgres psql -U tim -d tim -c '\dt custom.*'
  docker exec -it tim-postgres psql -U tim -d tim -c '\dt tara.*'
  ```

## Denylist/allowlist not effective
- Ensure your verification layer computes the same token hash format (e.g., `sha256:<hex>`).
- Denylist should be checked **before** allowlist in business logic.

## TARA login fails after redirect
- Expired or missing entry in `tara.oauth_state`. Verify TTL and clock skew.
- Ensure `tara.oidc.enabled=true` and client registration properties are set when you actually enable TARA.
