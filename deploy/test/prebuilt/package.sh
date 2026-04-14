#!/usr/bin/env bash
set -euo pipefail

TAG="${1:-test}"
SKIP_BUILD="${SKIP_BUILD:-0}"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd -- "${SCRIPT_DIR}/../../.." && pwd)"

PREBUILT_DIR="${SCRIPT_DIR}"
BUNDLE_DIR="${PREBUILT_DIR}/_bundle"

BACKEND_DIR="${PREBUILT_DIR}/backend"
WEB_DIR="${PREBUILT_DIR}/web"
SCHEMA_SQL="${PREBUILT_DIR}/schema.mysql.sql"

BACKEND_REPO="${REPO_ROOT}/VibeCampus-Backend"
FRONTEND_REPO="${REPO_ROOT}/VibeCampus-Frontend"

mkdir -p "${BUNDLE_DIR}" "${BACKEND_DIR}" "${WEB_DIR}"

if [[ "${SKIP_BUILD}" != "1" ]]; then
  echo "Building backend jar..."
  (cd "${BACKEND_REPO}" && mvn -pl vibecampus-bootstrap -am clean package -DskipTests)
  JAR="$(ls -t "${BACKEND_REPO}/vibecampus-bootstrap/target"/vibecampus-bootstrap-*.jar 2>/dev/null | head -n 1 || true)"
  [[ -n "${JAR}" ]] || { echo "Backend jar not found under vibecampus-bootstrap/target"; exit 1; }
  cp -f "${JAR}" "${BACKEND_DIR}/app.jar"

  echo "Building frontend dist..."
  (cd "${FRONTEND_REPO}" && VITE_API_BASE_URL="/api" npm ci && npm run build)
  [[ -d "${FRONTEND_REPO}/dist" ]] || { echo "Frontend dist not found at ${FRONTEND_REPO}/dist"; exit 1; }
  rm -rf "${WEB_DIR}/dist"
  cp -R "${FRONTEND_REPO}/dist" "${WEB_DIR}/dist"
fi

BACKEND_IMAGE="vibecampus-backend:${TAG}"
WEB_IMAGE="vibecampus-web:${TAG}"

echo "Building docker images..."
docker build -t "${BACKEND_IMAGE}" "${BACKEND_DIR}"
docker build -t "${WEB_IMAGE}" "${WEB_DIR}"
docker pull mysql:8.0 >/dev/null

IMAGES_TAR="${BUNDLE_DIR}/images-${TAG}.tar"
echo "Saving images to ${IMAGES_TAR} ..."
docker save -o "${IMAGES_TAR}" "${BACKEND_IMAGE}" "${WEB_IMAGE}" mysql:8.0

BUNDLE_TGZ="${BUNDLE_DIR}/prebuilt-${TAG}.tgz"
echo "Packing deploy bundle to ${BUNDLE_TGZ} ..."
STAGE_ROOT="${BUNDLE_DIR}/stage"
STAGE_DIR="${STAGE_ROOT}/vibecampus-prebuilt"
rm -rf "${STAGE_ROOT}"
mkdir -p "${STAGE_DIR}/backend" "${STAGE_DIR}/web" "${STAGE_DIR}/_bundle"

cp -f "${PREBUILT_DIR}/docker-compose.images.yml" "${STAGE_DIR}/docker-compose.images.yml"
cp -f "${PREBUILT_DIR}/.env.example" "${STAGE_DIR}/.env.example"
cp -f "${SCHEMA_SQL}" "${STAGE_DIR}/schema.mysql.sql"
cp -f "${BACKEND_DIR}/app.jar" "${STAGE_DIR}/backend/app.jar"
cp -f "${WEB_DIR}/default.conf" "${STAGE_DIR}/web/default.conf"
cp -R "${WEB_DIR}/dist" "${STAGE_DIR}/web/dist"
cp -f "${IMAGES_TAR}" "${STAGE_DIR}/_bundle/images-${TAG}.tar"

(cd "${STAGE_ROOT}" && tar -czf "${BUNDLE_TGZ}" "vibecampus-prebuilt")

echo
echo "DONE"
echo "Bundle: ${BUNDLE_TGZ}"
echo "Server: tar -xzf prebuilt-${TAG}.tgz && cd vibecampus-prebuilt && cp .env.example .env && docker load -i _bundle/images-${TAG}.tar && docker compose -f docker-compose.images.yml --env-file .env up -d"

