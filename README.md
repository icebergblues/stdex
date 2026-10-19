# Examen Final — Microservicios con Quarkus

Microservicio `app-todos` (Quarkus 3.37.2) con dos réplicas, registro
dinámico en Consul, API Gateway Traefik 3.x, balanceo round-robin y
scraping Prometheus.

## Arquitectura

```
        ┌────────────────────────────────────────────────────┐
        │                   Traefik 3.x                       │
        │  Entrypoint web:8080   Dashboard:8083              │
        │  Provider: consulCatalog (refresco 5s)             │
        └────────────────────────────────────────────────────┘
                            │
                            ▼
        ┌────────────────────────────────────────────────────┐
        │                       Consul                        │
        │  app-todos-1  (reg)  app-todos-2  (reg)             │
        └────────────────────────────────────────────────────┘
              ▲                              ▲
              │ registerService              │ registerService
              │                              │
   ┌──────────────────────┐       ┌──────────────────────┐
   │   app-todos-1        │       │   app-todos-2        │
   │  INSTANCE_ID=        │       │  INSTANCE_ID=        │
   │   app-todos-1        │       │   app-todos-2        │
   │  http: 8080          │       │  http: 8080          │
   │  Postgres + REST     │       │  Postgres + REST     │
   │  Client jsonplaceholder                                 │
   └──────────┬───────────┘       └──────────┬───────────┘
              │                              │
              └──────────┬───────────────────┘
                         ▼
                 ┌─────────────────┐
                 │   PostgreSQL    │
                 │   todos-db      │
                 └─────────────────┘

        ┌────────────────────┐
        │    Prometheus      │  /q/metrics
        │   :9090            │  app-todos-1:8080
        └────────────────────┘  app-todos-2:8080
```

## Componentes

| Servicio       | Imagen                  | Host port | Notas                                      |
| -------------- | ----------------------- | --------- | ------------------------------------------ |
| `postgres`     | `postgres:17.7`         | `54321`   | Volumen `postgres_data`                    |
| `consul`       | `consul:1.15.4`         | `8500`    | Modo dev, UI habilitada                    |
| `app-todos-1`  | `jaimesalvador/app-todos:latest` | `8081` | `APP_INSTANCE_ID=app-todos-1`            |
| `app-todos-2`  | `jaimesalvador/app-todos:latest` | `8082` | `APP_INSTANCE_ID=app-todos-2`            |
| `traefik`      | `traefik:v3.3`          | `8080`, `8083` | Entrypoints `web` y `dashboard`     |
| `prometheus`   | `prom/prometheus`       | `9090`    | `prometheus.yml` con las 2 réplicas        |

## Endpoints `app-todos`

| Método  | Ruta                       | Descripción                                                |
| ------- | -------------------------- | ---------------------------------------------------------- |
| `GET`   | `/todos`                   | Lista todos los TODOs enriquecidos con `userName`          |
| `GET`   | `/todos/{id}`              | Devuelve un TODO por id                                    |
| `POST`  | `/todos`                   | Crea un TODO (201 + recurso)                               |
| `PUT`   | `/todos/{id}`              | Actualiza un TODO (200 / 404)                              |
| `DELETE`| `/todos/{id}`              | Elimina un TODO (204 / 404)                                |
| `GET`   | `/q/health/live`           | Liveness MicroProfile Health                               |
| `GET`   | `/q/health/ready`          | Readiness MicroProfile Health                              |
| `GET`   | `/q/metrics`               | Métricas Prometheus                                        |
| `GET`   | `/instance`                | Devuelve `{ instanceId, port }` para verificar la réplica  |
| `GET`   | `/ping`                    | Health check HTTP usado por Consul                         |

> Acceso a través de Traefik: `http://localhost:8080/app-todos/todos`

## REST Client

La interfaz `UsersRestClient` consume `https://jsonplaceholder.typicode.com/users/{id}`.
Si la llamada falla se devuelve `userName = "unknown"` mediante
`@Fallback`. El `userName` no se loguea ni se expone en métricas (PII).

## Compilar y publicar

```bash
# 1) Compilar la app
./gradlew :app-todos:quarkusBuild

# 2) Construir y publicar la imagen Docker
docker login
docker build -f src/main/docker/Dockerfile.jvm -t jaimesalvador/app-todos:latest .
docker push jaimesalvador/app-todos:latest
```

> Sustituir `jaimesalvador` por el usuario real de Docker Hub.

## Despliegue

```bash
docker compose up -d
docker compose ps
docker compose logs -f app-todos-1
```

## Verificación

### 1. Consul — 2 réplicas registradas

UI: <http://localhost:8500/ui/dc1/services> → deben aparecer
`app-todos-1` y `app-todos-2` con sus health checks `passing`.

```bash
curl -s http://localhost:8500/v1/agent/services | jq
```

### 2. Traefik — router `app-todos` con 2 servidores

Dashboard: <http://localhost:8083/dashboard/#/http/routers>
debe existir el router `router-app-todos` con la regla
`PathPrefix('/app-todos')` y dos servidores detrás.

### 3. Balanceo round-robin

```bash
for i in $(seq 1 6); do
  curl -s http://localhost:8080/app-todos/instance
  echo
done
```

Cada respuesta alterna entre `instanceId=app-todos-1` y `instanceId=app-todos-2`.

### 4. Alta disponibilidad

```bash
docker compose stop app-todos-1
for i in $(seq 1 4); do
  curl -s http://localhost:8080/app-todos/todos | head -c 80
  echo
done
docker compose start app-todos-1
```

Tras unos segundos (refresco de 5s) la réplica caída desaparece del
pool de Traefik y las peticiones siguen respondiendo.

### 5. Prometheus

UI: <http://localhost:9090/targets> → los targets
`app-todos-1:8080` y `app-todos-2:8080` deben estar en estado `UP`.

Query de ejemplo:

```promql
http_server_requests_seconds_count{application="app-todos"}
```

## Entregables del examen

1. Proyecto Gradle autocontenido (`./gradlew :app-todos:quarkusBuild`).
2. `docker-compose.yml` con los 6 servicios.
3. `prometheus.yml` con el scrape de las 2 réplicas.
4. Imagen Docker Hub: <https://hub.docker.com/r/jaimesalvador/app-todos>

## URL Docker Hub

Una vez publicada:

```
https://hub.docker.com/r/<cuenta-dockerhub>/app-todos/tags
```
