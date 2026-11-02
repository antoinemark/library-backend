#!/usr/bin/env bash
# =============================================================================
# Gera a imagem Docker do backend (library-backend) e salva localmente em
# <OUTPUT_DIR>/<IMAGE_NAME>_<TAG>.tar.gz (docker save + gzip), com o .sha256 ao lado.
# Pode ser executado de qualquer diretório: o build sempre parte da raiz do repo.
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
DEFAULT_IMAGE_NAME="library-backend"

usage() {
  cat <<EOF
Uso: $(basename "$0") [TAG]

Builda a imagem Docker do ${DEFAULT_IMAGE_NAME} e salva em um arquivo .tar.gz local.

Argumentos:
  TAG            tag da imagem. Padrão: variável TAG; se ausente, 'git describe --always --dirty';
                 sem git, 'dev'. A imagem também recebe a tag 'latest'.

Variáveis de ambiente:
  TAG            tag da imagem (o argumento tem prioridade)
  IMAGE_NAME     nome da imagem             (padrão: ${DEFAULT_IMAGE_NAME})
  OUTPUT_DIR     diretório dos arquivos     (padrão: <repo>/images)

Gera:
  <OUTPUT_DIR>/<IMAGE_NAME>_<TAG>.tar.gz          imagem salva (name:TAG e name:latest)
  <OUTPUT_DIR>/<IMAGE_NAME>_<TAG>.tar.gz.sha256   checksum para conferência

Exemplos:
  $(basename "$0")
  $(basename "$0") 1.0.0
  OUTPUT_DIR=/tmp/imagens IMAGE_NAME=meu-backend $(basename "$0") demo
EOF
}

error() { echo "erro: $*" >&2; exit 1; }

# ---- argumentos ------------------------------------------------------------
case "${1:-}" in
  -h|--help) usage; exit 0 ;;
  -*) usage >&2; error "opção desconhecida: $1" ;;
esac
[[ $# -le 1 ]] || { usage >&2; error "informe no máximo um argumento (a TAG)"; }

IMAGE_NAME="${IMAGE_NAME:-$DEFAULT_IMAGE_NAME}"
OUTPUT_DIR="${OUTPUT_DIR:-$REPO_DIR/images}"

if [[ -n "${1:-}" ]]; then
  TAG="$1"
elif [[ -z "${TAG:-}" ]]; then
  # Tag automática a partir do git; caracteres inválidos para tag Docker viram "-"
  TAG="$(git -C "$REPO_DIR" describe --always --dirty 2>/dev/null || echo dev)"
  TAG="$(printf '%s' "$TAG" | tr -c 'A-Za-z0-9_.-' '-')"
fi

[[ "$TAG" =~ ^[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$ ]] \
  || error "tag inválida: '$TAG' (use letras, números, '_', '.' e '-'; até 128 caracteres)"

# ---- pré-requisitos --------------------------------------------------------
command -v docker >/dev/null 2>&1 || error "docker não encontrado no PATH."
docker info >/dev/null 2>&1 || error "não foi possível falar com o daemon do Docker (ele está rodando? seu usuário tem permissão?)."
command -v gzip >/dev/null 2>&1 || error "gzip não encontrado no PATH."
[[ -f "$REPO_DIR/Dockerfile" ]] || error "Dockerfile não encontrado em $REPO_DIR."

if command -v sha256sum >/dev/null 2>&1; then
  sha256() { sha256sum "$1" | awk '{print $1}'; }
elif command -v shasum >/dev/null 2>&1; then
  sha256() { shasum -a 256 "$1" | awk '{print $1}'; }
else
  error "sha256sum (ou shasum) não encontrado no PATH."
fi

mkdir -p "$OUTPUT_DIR"
OUTPUT_DIR="$(cd "$OUTPUT_DIR" && pwd)"

TAGS=("$IMAGE_NAME:$TAG")
[[ "$TAG" == "latest" ]] || TAGS+=("$IMAGE_NAME:latest")

# Nome de arquivo seguro mesmo se IMAGE_NAME tiver registry/namespace (ex.: ghcr.io/x/y)
FILE_NAME="$(printf '%s' "${IMAGE_NAME}_${TAG}" | tr '/:' '__').tar.gz"
OUTPUT_FILE="$OUTPUT_DIR/$FILE_NAME"

# ---- build -----------------------------------------------------------------
BUILD_ARGS=(
  --label "org.opencontainers.image.version=$TAG"
  --label "org.opencontainers.image.created=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
)
REVISION="$(git -C "$REPO_DIR" rev-parse HEAD 2>/dev/null || true)"
[[ -z "$REVISION" ]] || BUILD_ARGS+=(--label "org.opencontainers.image.revision=$REVISION")
for t in "${TAGS[@]}"; do BUILD_ARGS+=(-t "$t"); done

echo "==> Buildando ${TAGS[*]} a partir de $REPO_DIR"
DOCKER_BUILDKIT=1 docker build "${BUILD_ARGS[@]}" -f "$REPO_DIR/Dockerfile" "$REPO_DIR"

# ---- save ------------------------------------------------------------------
echo "==> Salvando em $OUTPUT_FILE"
TMP_FILE="$OUTPUT_FILE.tmp"
trap 'rm -f "$TMP_FILE"' EXIT
docker save "${TAGS[@]}" | gzip -c > "$TMP_FILE"
mv -f "$TMP_FILE" "$OUTPUT_FILE"
trap - EXIT

# Checksum com o nome relativo, para conferir com: (cd OUTPUT_DIR && sha256sum -c ARQUIVO.sha256)
printf '%s  %s\n' "$(sha256 "$OUTPUT_FILE")" "$FILE_NAME" > "$OUTPUT_FILE.sha256"

SIZE="$(du -h "$OUTPUT_FILE" | cut -f1)"
echo
echo "Imagem gerada com sucesso:"
echo "  tags:     ${TAGS[*]}"
echo "  arquivo:  $OUTPUT_FILE ($SIZE)"
echo "  sha256:   $OUTPUT_FILE.sha256"
echo
echo "Para carregar em outra máquina:"
echo "  docker load -i \"$OUTPUT_FILE\""
