# syntax=docker/dockerfile:1

# =============================================================================
# Etapa 1: build do jar com Maven
# =============================================================================
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# Copia só o pom.xml primeiro: enquanto as dependências não mudarem, esta camada
# fica em cache e o download não se repete a cada alteração no código-fonte.
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp dependency:go-offline

# Código-fonte e empacotamento (testes ficam para o pipeline / execução local).
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp package -DskipTests \
 && cp target/*.jar /build/app.jar

# =============================================================================
# Etapa 2: runtime enxuto (somente JRE)
# =============================================================================
FROM eclipse-temurin:21-jre-alpine

LABEL org.opencontainers.image.title="library-backend" \
      org.opencontainers.image.description="API REST do sistema de empréstimo de livros (Spring Boot 4 / Java 21 / H2 em memória)" \
      org.opencontainers.image.source="https://github.com/antoinemark/library-backend"

# Usuário sem privilégios para rodar a aplicação
RUN addgroup -S app && adduser -S -G app -H -s /sbin/nologin app

WORKDIR /app

COPY --from=build --chown=app:app /build/app.jar /app/app.jar

USER app

EXPOSE 8080

# Opções extras da JVM (ex.: -Xmx512m -XX:MaxRAMPercentage=75)
ENV JAVA_OPTS=""

# Fuso horário da JVM. O container usa UTC por padrão, e entre 21h e 24h (Brasília) o "hoje"
# da aplicação (status dos empréstimos, devolução, datas do seed) já seria o dia seguinte.
ENV TZ=America/Sao_Paulo

# Usa 127.0.0.1 em vez de "localhost": no Alpine o wget do busybox pode resolver
# localhost para ::1 e falhar se o IPv6 estiver indisponível no container.
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/genres || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
