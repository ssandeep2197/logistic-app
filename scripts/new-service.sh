#!/usr/bin/env bash
# Scaffold a new Spring Boot backend microservice.
#   ./scripts/new-service.sh widgets 8011
# Creates backend/widgets-service/ wired to platform-lib.
set -euo pipefail

if [[ $# -ne 2 ]]; then
  echo "usage: $0 <name-without-service-suffix> <port>" >&2
  exit 1
fi

NAME="$1"
PORT="$2"
SVC="${NAME}-service"
PKG="com.helloworlds.tms.${NAME}"
DIR="backend/${SVC}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

if [[ -d "$ROOT/$DIR" ]]; then echo "already exists: $DIR" >&2; exit 1; fi

mkdir -p "$ROOT/$DIR/src/main/java/${PKG//.//}"
mkdir -p "$ROOT/$DIR/src/main/resources"

CLASS="$(echo "$NAME" | sed 's/^./\U&/')ServiceApplication"

cat > "$ROOT/$DIR/build.gradle.kts" <<KTS
plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    java
}

description = "${SVC}"

dependencies {
    implementation(project(":platform-lib:core"))
    implementation(project(":platform-lib:security"))
    implementation(project(":platform-lib:web"))
    implementation(project(":platform-lib:observability"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
KTS

cat > "$ROOT/$DIR/src/main/java/${PKG//.//}/${CLASS}.java" <<JAVA
package ${PKG};

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ${CLASS} {
    public static void main(String[] args) { SpringApplication.run(${CLASS}.class, args); }
}
JAVA

cat > "$ROOT/$DIR/src/main/resources/application.yml" <<YAML
spring: { application: { name: ${SVC} } }
server: { port: \${SERVER_PORT:${PORT}}, shutdown: graceful }
management:
  endpoints: { web: { exposure: { include: health,info,metrics,prometheus } } }
tms:
  security:
    jwt:
      signing-key: \${JWT_SIGNING_KEY:local-dev-key-change-me-please-at-least-32-bytes}
      issuer: tms-identity
YAML

echo "✔ Created $DIR"
echo "Next steps:"
echo "  1. Add include(\":${SVC}\") to backend/settings.gradle.kts"
echo "  2. Copy infra/k8s/base/identity-service → infra/k8s/base/${SVC}, edit names + ports"
echo "  3. Copy infra/k8s/overlays/prod/identity-service → infra/k8s/overlays/prod/${SVC}"
echo "  4. Add to .github/workflows/ci.yml springboot matrix and detect filters"
echo "  5. Add a route prefix in backend/api-gateway/src/main/resources/application.yml"
