[![Docker Image Version](https://img.shields.io/docker/v/tolgee/tolgee/latest?label=docker&logo=docker&logoColor=fff&color=2496ED)](https://hub.docker.com/r/tolgee/tolgee/tags)
[![GitHub Release](https://img.shields.io/github/v/release/tolgee/tolgee-platform?label=release)](https://github.com/tolgee/tolgee-platform/releases/latest)
[![License](https://img.shields.io/badge/license-Apache%202%20%2F%20Tolgee%20EL-blue)](https://github.com/tolgee/tolgee-platform/blob/main/LICENSE)
[![Docs](https://img.shields.io/badge/docs-tolgee.io-EC407A)](https://docs.tolgee.io/platform/self_hosting/running_with_docker)
[![Slack](https://img.shields.io/badge/Slack-4A154B?logo=slack&logoColor=fff)](https://join.slack.com/t/tolgeecommunity/shared_invite/zt-2zp55d175-_agXTfKKVbf1BYXlKlmwbA)

# Tolgee — open-source localization platform

[<img src="https://raw.githubusercontent.com/tolgee/documentation/main/tolgee_logo_text.svg" alt="Tolgee" width="180" />](https://tolgee.io)

An open-source alternative to Crowdin, Phrase and Lokalise. This image runs the entire
Tolgee server — web app, REST API, background workers and an optional bundled
PostgreSQL — in a single container.

`linux/amd64` · `linux/arm64` · JRE 25 on Alpine

---

## Quick start

```bash
docker run -v tolgee_data:/data -p 8085:8080 tolgee/tolgee
```

Open <http://localhost:8085>. Sign in as `admin` — the initial password is generated on
first boot and written into the volume:

```bash
docker exec <container> cat /data/initial.pwd
```

> **The bundled PostgreSQL is for evaluation only.** For anything you care about, run an
> external database — see **Production setup** below.

## Production setup (docker compose)

`docker-compose.yaml`:

```yaml
services:
  app:
    image: tolgee/tolgee:latest
    volumes:
      - ./data:/data
      - ./config.yaml:/config.yaml
    ports:
      - '8080:8080'
    environment:
      spring.config.additional-location: file:///config.yaml
    depends_on:
      - db
  db:
    image: postgres:13
    environment:
      POSTGRES_DB: postgres
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: change_me
    volumes:
      - ./data/postgres:/var/lib/postgresql/data
```

`config.yaml`:

```yaml
tolgee:
  postgres-autostart:
    enabled: false          # turn off the bundled database
  authentication:
    enabled: true
    jwt-secret: change_me   # keeps sessions valid across restarts
spring:
  datasource:
    url: jdbc:postgresql://db:5432/postgres
    username: postgres
    password: change_me
```

```bash
docker compose up -d
```

Full walkthrough: [Running with Docker](https://docs.tolgee.io/platform/self_hosting/running_with_docker).

## Tags

| Tag | Use it for |
| --- | --- |
| `latest` | Tracks the newest release. Fine for trying Tolgee out. |
| `v3.218.6` — every release | Immutable. **Pin one of these in production.** |

Every tag is a multi-arch manifest for `linux/amd64` and `linux/arm64`.
Releases go out several times a week — see the
[changelog](https://github.com/tolgee/tolgee-platform/releases).

## Image reference

| | |
| --- | --- |
| **Port 8080** | Web app + REST API |
| **Port 25432** | Bundled PostgreSQL, when `postgres-autostart` is on |
| **Volume `/data`** | Database files, uploaded screenshots, `initial.pwd` |
| **Healthcheck** | `GET /actuator/health` (already declared in the image) |
| **Base** | `postgres:13-alpine` + Eclipse Temurin JRE 25 |
| **Config** | `TOLGEE_*` env vars, or mount a YAML file and point `spring.config.additional-location` at it |

## Common configuration

| Variable | What it does |
| --- | --- |
| `TOLGEE_AUTHENTICATION_ENABLED` | Turn authentication on (do this). |
| `TOLGEE_AUTHENTICATION_INITIAL_USERNAME` | First admin account. Defaults to `admin`. |
| `TOLGEE_AUTHENTICATION_INITIAL_PASSWORD` | Set it yourself, or read the generated one from `/data/initial.pwd`. |
| `TOLGEE_AUTHENTICATION_JWT_SECRET` | Signs sessions. Set it, or everyone is logged out on restart. |
| `TOLGEE_POSTGRES_AUTOSTART_ENABLED` | `false` to use your own PostgreSQL. |
| `SPRING_DATASOURCE_URL` | Your database, e.g. `jdbc:postgresql://db:5432/postgres`. |
| `TOLGEE_SMTP_HOST` and friends | Outgoing mail — invitations, password resets. |
| `TOLGEE_FILE_STORAGE_*` | Store screenshots in S3 instead of the volume. |
| `OTEL_JAVAAGENT_ENABLED` | `true` starts the bundled OpenTelemetry agent for traces. |

Every property, in env / YAML / properties form:
[configuration reference](https://docs.tolgee.io/platform/self_hosting/configuration).

## What you get

**Translating**
Web editor with comments, translation history and an activity log · translation memory
with similarity scoring · glossaries · machine translation via DeepL, Google Translate,
AWS Translate and LLM providers · auto-translation of new keys · review and task
workflows.

**In your app**
In-context editing — ALT-click a string in your running app and edit it, production
included · one-click screenshots with the phrases highlighted · SDKs for React, Vue,
Svelte, Angular, Next.js, Nuxt, i18next and more · content delivery to a CDN ·
webhooks · a CLI · a Figma plugin, a VS Code extension and a Chrome extension.

**For teams**
Organizations, granular permissions and SSO/SAML · branching, so translations follow
your feature branches · batch operations across thousands of keys · an
[MCP server](https://github.com/tolgee/tolgee-platform/blob/main/DEVELOPMENT.md#mcp-server)
so AI coding assistants can manage translations directly.

## Licensing

The platform is Apache 2.0. The `ee/` directory is covered by the
[Tolgee Enterprise License](https://github.com/tolgee/tolgee-platform/blob/main/ee/LICENSE)
and its features need a license key — everything else in this image is free to
self-host, with no seat or key limit. Details:
[self-hosted licensing](https://docs.tolgee.io/platform/self_hosting/licensing).

## Links

[Documentation](https://docs.tolgee.io/) ·
[GitHub](https://github.com/tolgee/tolgee-platform) ·
[Report a bug](https://github.com/tolgee/tolgee-platform/issues) ·
[Slack community](https://join.slack.com/t/tolgeecommunity/shared_invite/zt-2zp55d175-_agXTfKKVbf1BYXlKlmwbA) ·
[Tolgee Cloud](https://app.tolgee.io/sign_up)
