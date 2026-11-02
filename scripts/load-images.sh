#!/usr/bin/env bash
# =============================================================================
# Carrega no Docker (docker load) todas as imagens .tar.gz de um diretório,
# conferindo antes o checksum do arquivo .sha256 correspondente, quando existir.
# Ao final, a tag 'latest' de cada imagem aponta para a versão buildada por último.
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

usage() {
  cat <<EOF
Uso: $(basename "$0") [DIRETORIO]

Executa 'docker load' para cada arquivo *.tar.gz do diretório. Se existir <arquivo>.sha256
ao lado, o checksum é validado antes; divergência interrompe o script sem carregar o arquivo.
Se houver mais de um arquivo da mesma imagem (ex.: builds de dias diferentes), a tag 'latest'
(usada pelo docker-compose.yml) fica com a versão buildada por último, e não com o último
arquivo carregado.

Argumentos:
  DIRETORIO      onde estão as imagens. Padrão: variável OUTPUT_DIR; se ausente, <repo>/images

Exemplo:
  $(basename "$0") /media/pendrive/imagens
EOF
}

error() { echo "erro: $*" >&2; exit 1; }
warn() { echo "aviso: $*" >&2; }

case "${1:-}" in
  -h|--help) usage; exit 0 ;;
  -*) usage >&2; error "opção desconhecida: $1" ;;
esac
[[ $# -le 1 ]] || { usage >&2; error "informe no máximo um argumento (o diretório)"; }

IMAGES_DIR="${1:-${OUTPUT_DIR:-$REPO_DIR/images}}"
[[ -d "$IMAGES_DIR" ]] || error "diretório não encontrado: $IMAGES_DIR"
IMAGES_DIR="$(cd "$IMAGES_DIR" && pwd)"

command -v docker >/dev/null 2>&1 || error "docker não encontrado no PATH."
docker info >/dev/null 2>&1 || error "não foi possível falar com o daemon do Docker (ele está rodando? seu usuário tem permissão?)."

if command -v sha256sum >/dev/null 2>&1; then
  sha256() { sha256sum "$1" | awk '{print $1}'; }
elif command -v shasum >/dev/null 2>&1; then
  sha256() { shasum -a 256 "$1" | awk '{print $1}'; }
else
  sha256() { return 1; }
  warn "sha256sum/shasum não encontrado: os checksums não serão validados."
fi

shopt -s nullglob
FILES=("$IMAGES_DIR"/*.tar.gz)
shopt -u nullglob
[[ ${#FILES[@]} -gt 0 ]] || error "nenhum arquivo .tar.gz encontrado em $IMAGES_DIR"

# Uma linha "repositorio data-de-criacao id" para cada imagem carregada. O 'docker load' move a
# tag 'latest' para o último arquivo carregado (ordem alfabética), que não é necessariamente o
# build mais recente; com esta lista, a 'latest' é reposicionada no final.
LOADED=()

echo "==> ${#FILES[@]} arquivo(s) em $IMAGES_DIR"
for file in "${FILES[@]}"; do
  name="$(basename "$file")"
  echo
  echo "--> $name"

  if [[ -f "$file.sha256" ]]; then
    expected="$(awk 'NR==1 {print tolower($1)}' "$file.sha256")"
    if actual="$(sha256 "$file")"; then
      [[ -n "$expected" && "$actual" == "$expected" ]] \
        || error "checksum NÃO confere para $name (esperado: ${expected:-vazio}, obtido: $actual). Arquivo corrompido ou incompleto?"
      echo "    checksum ok"
    fi
  else
    warn "$name sem arquivo .sha256 — carregando sem validar."
  fi

  output="$(docker load -i "$file")"
  printf '%s\n' "$output"

  # Consulta cada imagem logo após o load, antes que um arquivo seguinte leve a tag 'latest'
  while IFS= read -r ref; do
    [[ -n "$ref" ]] || continue
    LOADED+=("${ref%:*} $(docker image inspect -f '{{.Created}} {{.Id}}' "$ref")")
  done < <(printf '%s\n' "$output" | sed -n 's/^Loaded image: //p')
done

if [[ ${#LOADED[@]} -gt 0 ]]; then
  echo
  echo "==> Tag 'latest' apontando para o build mais recente de cada imagem:"
  # Por repositório, a imagem com a maior data de criação (ISO-8601 em UTC, ordenável como texto)
  printf '%s\n' "${LOADED[@]}" | LC_ALL=C sort -k1,1 -k2,2r | awk '!seen[$1]++' |
    while read -r repo created id; do
      docker tag "$id" "$repo:latest"
      echo "    $repo:latest -> imagem criada em $created"
    done
fi

echo
echo "Imagens carregadas com sucesso. Para subir a stack:"
echo "  docker compose -f \"$REPO_DIR/docker-compose.yml\" up -d"
