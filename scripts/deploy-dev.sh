#!/usr/bin/env bash
set -euo pipefail

# Build, publish, and deploy one immutable Tolgee DEV release.
# Usage: ./scripts/deploy-dev.sh [image-tag]

REPO="${DOCKER_IMAGE_REPO:-377091008548.dkr.ecr.us-west-2.amazonaws.com/tolgee}"
KUBECONFIG_PATH="${KUBECONFIG_PATH:-$HOME/.kube/aws-dev/tripo-dev-service.kubeconfig}"
NAMESPACE="${K8S_NAMESPACE:-env-base}"
DEPLOYMENT="tolgee"
MANIFEST="k8s/tolgee.yaml"
ENV_FILE="${K8S_ENV_FILE:-k8s/tolgee.env}"
CONFIGMAP="tolgee-config"
HEALTHZ_URL="${HEALTHZ_URL:-https://tolgee.devops.tripo3d.ai/actuator/health}"

[[ -r "$KUBECONFIG_PATH" ]] || { echo "ERROR: kubeconfig is not readable: $KUBECONFIG_PATH" >&2; exit 1; }
[[ -r "$ENV_FILE" ]] || { echo "ERROR: env file is not readable: $ENV_FILE" >&2; exit 1; }
docker info >/dev/null 2>&1 || { echo "ERROR: Docker daemon is not running." >&2; exit 1; }

SHA=$(git rev-parse --short HEAD)
BRANCH=$(git branch --show-current | tr '/' '-')
TAG="${1:-${BRANCH}-${SHA}-$(date -u +%Y%m%d%H%M%S)}"
IMAGE="${REPO}:${TAG}"

echo "==> Image: $IMAGE"
aws ecr get-login-password --region us-west-2 | docker login --username AWS --password-stdin "${REPO%/*}"
VERSION="$TAG" DOCKER_IMAGE="$REPO" ./gradlew :server-app:dockerPublish

perl -0pi -e "s|image: \Q$REPO\E:[^\n]+|image: $IMAGE|" "$MANIFEST"

kubectl --kubeconfig "$KUBECONFIG_PATH" -n "$NAMESPACE" create configmap "$CONFIGMAP" \
  --from-env-file="$ENV_FILE" --dry-run=client -o yaml |
  kubectl --kubeconfig "$KUBECONFIG_PATH" -n "$NAMESPACE" apply -f -
kubectl --kubeconfig "$KUBECONFIG_PATH" apply -n "$NAMESPACE" -f <(awk 'BEGIN { RS="---\n" } $0 !~ /kind: Namespace/ { print "---\n" $0 }' "$MANIFEST")

kubectl --kubeconfig "$KUBECONFIG_PATH" -n "$NAMESPACE" rollout status "deployment/$DEPLOYMENT" --timeout=10m
curl -fsS "$HEALTHZ_URL"
echo "==> Deploy complete: $IMAGE"
