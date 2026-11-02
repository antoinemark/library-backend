#!/usr/bin/env bash
# =============================================================================
# Gera as imagens Docker do backend e do frontend e salva as duas no mesmo
# diretório (padrão: <library-backend>/images), usando o scripts/build-image.sh
# de cada repositório. O frontend é procurado em ../biblioteca-web-frontend.
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
FRONTEND_DIR="${FRONTEND_DIR:-$BACKEND_DIR/../biblioteca-web-frontend}"

usage() {
  cat <<EOF
Uso: $(basename "$0") [TAG]

Builda e salva as imagens do backend e do frontend (docker save + gzip) em um diretório comum.

Argumentos:
  TAG                   tag das duas imagens. Padrão: variável TAG; se ausente, cada
                        repositório usa o próprio 'git describe --always --dirty' (ou 'dev').

Variáveis de ambiente:
  TAG                   tag das imagens (o argumento tem prioridade)
  OUTPUT_DIR            diretório dos arquivos      (padrão: <library-backend>/images)
  FRONTEND_DIR          repositório do frontend     (padrão: ../biblioteca-web-frontend)
  BACKEND_IMAGE_NAME    nome da imagem do backend   (padrão: library-backend)
  FRONTEND_IMAGE_NAME   nome da imagem do frontend  (padrão: biblioteca-web-frontend)
  VITE_API_URL          repassada ao build do frontend (padrão do Dockerfile: /api)

Depois, para carregar as imagens em outra máquina:
  scripts/load-images.sh <diretório>
EOF
}

error() { echo "erro: $*" >&2; exit 1; }
warn() { echo "aviso: $*" >&2; }

case "${1:-}" in
  -h|--help) usage; exit 0 ;;
  -*) usage >&2; error "opção desconhecida: $1" ;;
esac
[[ $# -le 1 ]] || { usage >&2; error "informe no máximo um argumento (a TAG)"; }

[[ -z "${1:-}" ]] || TAG="$1"
if [[ -n "${TAG:-}" ]]; then export TAG; else unset TAG; fi

OUTPUT_DIR="${OUTPUT_DIR:-$BACKEND_DIR/images}"
mkdir -p "$OUTPUT_DIR"
OUTPUT_DIR="$(cd "$OUTPUT_DIR" && pwd)"
export OUTPUT_DIR

BACKEND_SCRIPT="$SCRIPT_DIR/build-image.sh"
FRONTEND_SCRIPT="$FRONTEND_DIR/scripts/build-image.sh"

# Marca o início desta execução, para o resumo separar os arquivos gerados agora dos antigos
RUN_MARKER="$(mktemp)"
trap 'rm -f "$RUN_MARKER"' EXIT

# IMAGE_NAME é definido por chamada para que um IMAGE_NAME exportado no shell
# não faça as duas imagens saírem com o mesmo nome.
echo "########## Backend ##########"
IMAGE_NAME="${BACKEND_IMAGE_NAME:-library-backend}" bash "$BACKEND_SCRIPT"

echo
echo "########## Frontend ##########"
FRONTEND_BUILT=false
if [[ -f "$FRONTEND_SCRIPT" ]]; then
  IMAGE_NAME="${FRONTEND_IMAGE_NAME:-biblioteca-web-frontend}" bash "$FRONTEND_SCRIPT"
  FRONTEND_BUILT=true
else
  warn "script do frontend não encontrado em $FRONTEND_SCRIPT — imagem do frontend NÃO foi gerada."
  warn "Clone o repositório biblioteca-web-frontend ao lado do library-backend ou defina FRONTEND_DIR."
fi

echo
echo "########## Resumo ##########"
echo "Arquivos gerados nesta execução em $OUTPUT_DIR:"
OLD_FILES=()
for f in "$OUTPUT_DIR"/*.tar.gz; do
  [[ -e "$f" ]] || continue
  if [[ "$f" -nt "$RUN_MARKER" ]]; then
    printf '  %s\t%s\n' "$(du -h "$f" | cut -f1)" "$(basename "$f")"
  else
    OLD_FILES+=("$(basename "$f")")
  fi
done
if [[ ${#OLD_FILES[@]} -gt 0 ]]; then
  warn "o diretório também tem arquivos de execuções anteriores (o load-images.sh carrega todos,"
  warn "mas a tag 'latest' fica com o build mais recente). Apague-os se não precisar deles:"
  for f in "${OLD_FILES[@]}"; do warn "  $f"; done
fi
[[ "$FRONTEND_BUILT" == true ]] || warn "somente a imagem do backend foi gerada (frontend ausente)."
echo
echo "Para carregar todas as imagens em outra máquina:"
echo "  $SCRIPT_DIR/load-images.sh \"$OUTPUT_DIR\""
echo "Para subir a stack com as imagens carregadas:"
echo "  docker compose -f \"$BACKEND_DIR/docker-compose.yml\" up -d"
