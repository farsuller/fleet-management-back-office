#!/bin/sh
set -eu

cat > /app/public/config.js <<EOF
window.__FLEET_CONFIG__ = Object.assign(
  {
    apiBaseUrl: "${FLEET_API_BASE_URL:-http://localhost:8080}",
    wsBaseUrl: "${FLEET_WS_BASE_URL:-ws://localhost:8080}"
  },
  window.__FLEET_CONFIG__ || {}
);
EOF

exec serve -s /app/public -l tcp://0.0.0.0:${PORT:-10000}