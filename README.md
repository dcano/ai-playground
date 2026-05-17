# AI Playground

A hands-on Spring AI laboratory demonstrating the core capabilities of the Spring AI
framework: chat, structured output, streaming, chat memory, RAG pipelines, function
calling, MCP servers/clients, observability, and agent skills.

## Table of Contents

- [Architecture](#architecture)
- [Modules](#modules)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [API Reference](#api-reference)
- [Observability](#observability)
- [Developer Expert Skill](#developer-expert-skill)

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        ai-playground (port 9191)                │
│  ┌──────────┐  ┌──────────┐  ┌──────┐  ┌─────┐  ┌──────────┐  │
│  │   Chat   │  │   RAG    │  │Tools │  │ MCP │  │ Stream / │  │
│  │ Memory   │  │ Pipeline │  │(fn)  │  │client│  │Structured│  │
│  └──────────┘  └──────────┘  └──────┘  └─────┘  └──────────┘  │
└──────────┬──────────┬────────────────────────────────────────────┘
           │          │
     ┌─────▼──┐  ┌────▼──────┐
     │ Ollama │  │Google GenAI│  (switchable via property)
     └────────┘  └───────────┘

┌───────────────────────┐   ┌─────────────────────────────────┐
│ ai-playground-mcp-    │   │ ai-playground-mcp-server-remote  │
│ server (stdio, 9192)  │   │ runtime (HTTP/SSE)               │
└───────────────────────┘   └─────────────────────────────────┘

┌────────────────────────────────────────────────────────────────┐
│              ai-playground-skills (port 9193)                  │
│  spring-ai-agent-utils SkillsTool · developer-expert skill     │
│  Spring Boot 4.x · Spring AI 2.0.0-M3 (standalone module)     │
└────────────────────────────────────────────────────────────────┘

Infrastructure (Docker Compose)
  PostgreSQL :5432 · Qdrant :6333/:6334 · Prometheus :9090
  Grafana :3000 · Jaeger :16686/:4317
```

---

## Modules

| Module | Description |
|---|---|
| `ai-playground-runtime` | Main Spring Boot application (port **9191**) |
| `ai-playground-web` | REST controllers (chat, RAG, tools, MCP client, stream, structured output) |
| `ai-playground-configuration` | `ChatClient` beans; Ollama / Google GenAI model switching |
| `ai-playground-advisors` | Custom `TokenUsageAuditAdvisor` |
| `ai-playground-tools` | Spring AI `@Tool` functions: help-desk tickets, local time |
| `ai-playground-rag` | RAG pipeline: query translation, Qdrant vector store, PII masking, web search |
| `ai-playground-prompt-templates` | StringTemplate (`.st`) system/user prompts and HR policy PDF |
| `ai-playground-model` | Shared domain model and configuration properties |
| `ai-playground-mcp-server` | MCP tool definitions (help desk, tickets) |
| `ai-playground-mcp-server-runtime` | Stdio MCP server (port **9192**) |
| `ai-playground-mcp-server-remote-runtime` | HTTP/SSE MCP server |
| `ai-playground-mcp-client` | MCP client connecting to stdio and remote MCP servers |
| `ai-playground-skills` | **Developer expert skill** via `spring-ai-agent-utils` (port **9193**, standalone) |

---

## Prerequisites

| Tool | Version |
|---|---|
| Java | 21+ |
| Maven | 3.9+ |
| Docker & Docker Compose | any recent |
| Ollama | latest (or Google GenAI credentials) |

### Required Ollama models

```bash
ollama pull gpt-oss:latest         # chat model
ollama pull embeddinggemma:300m    # embedding model (used by RAG)
```

---

## Getting Started

### 1. Start infrastructure

```bash
# Create local data directories expected by the volume mounts
mkdir -p ~/temp/data/postgres ~/temp/data/qdrant ~/temp/data/grafana

docker compose up -d
```

### 2. Build

```bash
./mvnw clean package -DskipTests
```

### 3. Run the main application

```bash
./mvnw spring-boot:run -pl ai-playground-runtime
```

The app starts on **http://localhost:9191**.

### 4. (Optional) Run the MCP stdio server

```bash
./mvnw spring-boot:run -pl ai-playground-mcp-server-runtime
```

### 5. (Optional) Run the developer expert skill service

The `ai-playground-skills` module is a standalone Spring Boot 4.x project and is
built separately:

```bash
cd ai-playground-skills
../mvnw spring-boot:run
```

Service starts on **http://localhost:9193**.

---

## Configuration

### Model selection

Set `playground.model.type` to switch providers:

| Value | Provider | Extra config needed |
|---|---|---|
| `ollama` *(default)* | Local Ollama | Ollama running on `localhost:11434` |
| `google-genai` | Google Gemini 2.5 Flash | `GOOGLE_GENAI_API_KEY`, `GOOGLE_PROJECT_ID` |

```bash
# Use Google GenAI
PLAYGROUND_MODEL_TYPE=google-genai \
GOOGLE_GENAI_API_KEY=<key> \
GOOGLE_PROJECT_ID=<project> \
./mvnw spring-boot:run -pl ai-playground-runtime
```

### Key environment variables

| Variable | Default | Description |
|---|---|---|
| `PLAYGROUND_MODEL_TYPE` | `ollama` | AI model provider |
| `GOOGLE_GENAI_API_KEY` | — | Google GenAI API key |
| `GOOGLE_PROJECT_ID` | — | Google Cloud project |
| `TAVILY_KEY` | `fake_key` | Tavily web-search API key (RAG web-search endpoint) |
| `TRACING_PROBABILITY` | `0.1` | OpenTelemetry trace sampling rate (0.0–1.0) |

### Chat memory

Persistent chat history uses H2 (file-based) by default:

```
spring.datasource.url=jdbc:h2:file:~/temp/data/h2/chatmemory
```

Enable memory for a conversation by passing a `conversationId` query parameter to
any `/api/chat-memory` endpoint.

---

## API Reference

All endpoints are served by the main app on **http://localhost:9191**.

### Chat

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/chat?message=` | Basic chat |
| `GET` | `/api/chat-help-desk?message=` | Chat with help-desk system prompt |
| `GET` | `/api/email?message=` | Generate a professional email |
| `GET` | `/api/prompt-stuffing?message=` | Prompt-stuffing demo |
| `GET` | `/api/stream?message=` | Server-sent event streaming response |

### Structured Output

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/chat-bean?message=` | Response mapped to a Java bean |
| `GET` | `/api/chat-list?message=` | Response as a `List` |
| `GET` | `/api/chat-map?message=` | Response as a `Map` |
| `GET` | `/api/chat-bean-list?message=` | Response as `List<Bean>` |

### Chat Memory

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/chat-memory?message=&conversationId=` | Chat with persistent history |

### Evaluation

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/evaluate/chat?message=` | Fact-checked chat response |
| `GET` | `/api/evaluate/prompt-stuffing?message=` | Fact-checked prompt-stuffing |

### RAG

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/rag/random/chat?message=` | RAG over random seed data |
| `GET` | `/api/rag/document/chat?message=` | RAG over HR policy PDF |
| `GET` | `/api/rag/managed/chat?message=` | Managed RAG pipeline |
| `GET` | `/api/rag/web-search/chat?message=` | RAG with Tavily web search |

### Tools (Function Calling)

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/tools/help-desk?message=` | Create support tickets via AI tool |
| `GET` | `/api/tools/local-time?message=` | Query local time via AI tool |

### MCP Client

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/mcp/chat?message=` | Chat delegated to an MCP server tool |

### Developer Expert Skill (port 9193)

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/developer-expert/chat` | Body: plain-text development question |
| `GET` | `/api/developer-expert/chat?question=` | Same, via query parameter |

---

## Observability

The main application exposes metrics at `/actuator/prometheus`.

| Service | URL | Purpose |
|---|---|---|
| Prometheus | http://localhost:9090 | Metrics scraping |
| Grafana | http://localhost:3000 | Dashboards (default credentials: `admin/admin`) |
| Jaeger | http://localhost:16686 | Distributed traces |
| H2 Console | http://localhost:9191/h2-console | Chat-memory database browser |

Tracing sampling defaults to 10 % (`TRACING_PROBABILITY=0.1`). Set to `1.0` during
local development to capture all traces.

---

## Developer Expert Skill

The `ai-playground-skills` module demonstrates the
[spring-ai-agent-utils](https://github.com/spring-ai-community/spring-ai-agent-utils)
`SkillsTool` pattern.

A **skill** is a Markdown file with YAML front-matter that describes when and how the
AI agent should apply a particular body of knowledge. The agent invokes the skill as a
tool call, receiving the full Markdown content as context before generating its answer.

The `developer-expert` skill covers:

- Java 8-21+, Spring Framework, Spring Boot, Spring AI
- Architecture patterns: microservices, DDD, hexagonal, CQRS
- API design, database optimisation, caching strategies
- Testing (TDD/BDD, JUnit 5, Testcontainers, Mockito)
- Observability, security (OWASP Top 10), DevOps / CI-CD

```
ai-playground-skills/src/main/resources/skills/developer-expert/SKILL.md
```

### Example

```bash
# How do I prevent N+1 queries in Spring Data JPA?
curl "http://localhost:9193/api/developer-expert/chat?question=How+do+I+prevent+N%2B1+in+Spring+Data+JPA"

# Code review via POST
curl -X POST http://localhost:9193/api/developer-expert/chat \
     -H "Content-Type: text/plain" \
     -d "Review this service method for SOLID violations: ..."
```

> **Note:** This module uses Spring Boot 4.x and Spring AI 2.0.0-M3, which are
> incompatible with the parent project (Spring Boot 3.x / Spring AI 1.1.2). It is
> therefore kept as a standalone Maven project and is not included in the parent
> multi-module build.
