#!/usr/bin/env bash
set -euo pipefail
set -a; [ -f .env ] && source .env; set +a
exec ./mvnw spring-boot:run "$@"
