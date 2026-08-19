# Progress

- Date: 2026-08-18
- Branch: main
- Changes: Added a DEV K8S deployment template for the existing Tolgee image, persistent `/data` storage, ConfigMap input, and deployment through the existing ALB at `tolgee.vast-internal.com` in `env-base`.
- Verification: `bash -n scripts/deploy-dev.sh` and `git diff --check` passed. Gradle task verification was blocked because no Java Runtime is installed. Automated deployment was not run because it requires company AWS credentials, kubeconfig, and cluster access.
- Remaining issues: ECR publication is blocked by proxy interruptions while fetching Docker Hub layers. The deployment now uses the official multi-architecture `tolgee/tolgee:latest` image directly; rollout is pending.

- Date: 2026-08-19
- Branch: main
- Changes: Switched the Tolgee Ingress from the unresolvable `vast-internal.com` hostname/ALB to the existing DEV wildcard domain `tolgee.devops.tripo3d.ai` and shared NGINX Ingress. No Deployment, Service, PVC, or other workload was changed.
- Verification: DNS resolves `tolgee.devops.tripo3d.ai`; manifest change is limited to the Tolgee Ingress routing configuration. Final HTTPS check is pending.
- Remaining issues: None. HTTPS health check returned HTTP 200 with `status: UP`.

- Date: 2026-08-19
- Branch: main
- Changes: Deployed Tolgee to the DEV EKS cluster `tripo-dev-service`, namespace `env-base`, with one replica, its own `tolgee` Service, PVC, ConfigMap, and ALB Ingress for `tolgee.vast-internal.com`. Existing workloads were not modified.
- Verification: `kubectl rollout status deployment/tolgee` passed; Pod is `1/1 Running`, `0` restarts, and the container is using the amd64-compatible official multi-architecture image `tolgee/tolgee:latest`. ALB Ingress reconciliation succeeded. The application health endpoint passed the Pod readiness probe.
- Remaining issues: External HTTPS curl from this workstation returned a network/DNS connection error, so public hostname reachability still needs verification from the user's network. ECR push remains incomplete; the running Pod pulls directly from Docker Hub.

- Date: 2026-08-18
- Branch: current working branch
- Changes: Verified the local Tolgee Docker import flow for nested JSON and restored the original PostgreSQL 11 Compose data configuration. No database backup or restore was used for this verification.
- Verification: `docker compose ps` reports all services running and the Tolgee health endpoint returns HTTP 200. Uploaded the nested JSON fixture through the local UI to project 1; preview showed 10 translations, import returned HTTP 200, and the translations page showed 10 keys including the nested `page1.*` keys.
- Remaining issues: `project_id=2` does not exist in the current database; use `http://localhost:8091/projects/1/import`. The Compose file has no functional diff from its original configuration.
