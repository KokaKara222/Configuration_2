set -eu
cd "$(dirname "$0")"
exec gradle --quiet run --args="$*"