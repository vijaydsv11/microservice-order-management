#!/usr/bin/env bash
set -euo pipefail

# Fast Maven build helper for Linux/macOS
# Usage:
#   ./build-fast.sh
#   ./build-fast.sh clean
#   ./build-fast.sh module <module-name>
#   ./build-fast.sh module <module-name> clean

THREADS="1C"
SKIP_TESTS="-DskipTests"

run_all() {
  local goal="compile"
  if [[ "${1:-}" == "clean" ]]; then
    goal="clean compile"
  fi

  echo "Running ${goal} for all modules ..."
  mvn -T "${THREADS}" ${SKIP_TESTS} ${goal}
}

run_module() {
  local module_name="${1:-}"
  local mode="${2:-}"

  if [[ -z "${module_name}" ]]; then
    echo "ERROR: Missing module name."
    echo "Example: ./build-fast.sh module inventory-service"
    exit 1
  fi

  if [[ "${mode}" == "clean" ]]; then
    echo "Running clean module build for ${module_name} ..."
    mvn -T "${THREADS}" ${SKIP_TESTS} -pl "${module_name}" -am clean compile
    return
  fi

  echo "Running fast module build for ${module_name} ..."
  mvn -T "${THREADS}" ${SKIP_TESTS} -pl "${module_name}" -am compile
}

case "${1:-}" in
  "")
    run_all
    ;;
  clean)
    run_all clean
    ;;
  module)
    run_module "${2:-}" "${3:-}"
    ;;
  *)
    echo "Unknown option: $1"
    echo "Usage: ./build-fast.sh [clean|module <name> [clean]]"
    exit 1
    ;;
esac
