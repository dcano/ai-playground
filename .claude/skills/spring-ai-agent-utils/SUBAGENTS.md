# spring-ai-agent-utils — Sub-agents (TaskTools + A2A)

The sub-agent system lets the primary `ChatClient` delegate work to other
agents — either **local** Claude-style markdown agents or **remote** A2A
agents — through a single `TaskTool` exposed to the model. The SPI lives in
the `-common` module; built-in implementations live in the main library
(`claude`) and the `-a2a` module.

---

## SPI — `spring-ai-agent-utils-common`

Package: `org.springaicommunity.agent.common.task.subagent`
Coordinates: `org.springaicommunity:spring-ai-agent-utils-common`

| Type | Kind | Purpose |
|---|---|---|
| `SubagentDefinition` (interface) | — | Identity + config: `getName()`, `getDescription()`, `getKind()`, `getReference()`, default `toSubagentRegistrations()` |
| `SubagentReference` (record) | — | `(String uri, String kind, Map<String,Object> metadata)`; convenience ctor `(uri, kind)` |
| `SubagentResolver` (interface) | — | `boolean canResolve(SubagentReference)`, `SubagentDefinition resolve(SubagentReference)` |
| `SubagentExecutor` (interface) | — | `String getKind()`, `String execute(TaskCall, SubagentDefinition)` |
| `SubagentType` (record) | — | `(SubagentResolver resolver, SubagentExecutor executor)`; `kind()` delegates to `executor.getKind()` |
| `TaskCall` (record) | — | Tool input: `description` (3–5 words), `prompt`, `subagent_type`, `model` (optional `provider:model`), `resume` (optional agent id), `run_in_background` (bool) |

Built-in kinds: `CLAUDE` (local markdown) and `A2A` (remote).

---

## TaskTool — `org.springaicommunity.agent.tools.task.TaskTool`

The single tool the model uses to delegate work. `build()` returns a
`ToolCallback` (so wire it via `.defaultToolCallbacks(...)`).

```java
TaskTool.builder()
    .subagentReferences(List<SubagentReference> refs)   // who can be called
    .subagentTypes(List<SubagentType> types)            // how each kind runs
    .taskRepository(new DefaultTaskRepository())        // tracks background tasks
    // .toolDescriptionTemplate(...)                     // optional override
    .build();                                            // ToolCallback
```

Behaviour: `TaskFunction.apply(TaskCall)` looks up the named subagent, picks
the matching `SubagentExecutor`, and either runs synchronously (returning the
agent's text) or stores a background `CompletableFuture` keyed by a UUID
`task_id` and returns the id immediately.

The builder **auto-registers built-in Claude subagents**: `general-purpose`,
`explore`, `plan`, and `bash` — they're available out of the box.

## TaskOutputTool — `org.springaicommunity.agent.tools.task.TaskOutputTool`

Retrieves output from a background task started by `TaskTool`.

```java
TaskOutputTool.builder()
    .taskRepository(taskRepository)   // same instance as TaskTool
    .build();                          // ToolCallback
```

Input record: `TaskOutputCall(String task_id, Boolean block, Long timeout)`.
Defaults: blocks 30 s; max timeout 10 min.

## TaskRepository

Package: `org.springaicommunity.agent.tools.task.repository`

```java
interface TaskRepository {
    BackgroundTask getTasks(String taskId);
    BackgroundTask putTask(String taskId, Supplier<String> taskExecution);
    void removeTask(String taskId);
    void clear();
}
```

`BackgroundTask` wraps a `CompletableFuture<String>` and exposes
`isCompleted()`, `hasError()`, `getStatus()` (`Running` / `Completed` /
`Failed:…`), `getResult()`, `getError()`, `waitForCompletion(long ms)`,
`cancel(boolean)`, `isCancelled()`.

`DefaultTaskRepository` ships with three constructors:
`()`, `(ExecutorService)`, `(ExecutorService, boolean ownsExecutor)`.
Backed by `ConcurrentHashMap` + cached daemon thread pool. Exposes
`clearCompletedTasks()` and `shutdown()` (60 s grace).

> **Always share one `TaskRepository` instance** between `TaskTool` and
> `TaskOutputTool` — otherwise background lookups will miss.

---

## Local Claude sub-agents — `org.springaicommunity.agent.tools.task.claude`

Define each sub-agent as a Markdown file with YAML front-matter, then expose
it through `ClaudeSubagentType`.

### Front-matter contract — `ClaudeSubagentDefinition`

```markdown
---
name: code-reviewer
description: Reviews diffs for SOLID violations and security issues
model: anthropic:claude-sonnet-4-6              # optional, provider:model
tools: Read,Grep,Glob                            # comma-separated allowlist
disallowedTools: Bash,Write                      # comma-separated denylist
skills: code-review,security-audit               # comma-separated skill names
permissionMode: default                          # default | acceptEdits | dontAsk | bypassPermissions | plan
---

You are a senior code reviewer...
```

Kind constant: `ClaudeSubagentDefinition.KIND = "CLAUDE"`.

### Wiring a Claude sub-agent type

```java
SubagentType claudeType = ClaudeSubagentType.builder()
    .chatClientBuilder("default", defaultChatBuilder)      // REQUIRED key
    .chatClientBuilder("anthropic", anthropicBuilder)      // optional, used for provider:model overrides
    .skillsDirectories(List.of("/skills"))                  // optional
    .braveApiKey(System.getenv("BRAVE_API_KEY"))            // optional; enables BraveWebSearchTool
    .build();
```

Default tools auto-attached by `ClaudeSubagentType`: `TodoWriteTool`,
`GrepTool`, `GlobTool`, `ShellTools`, `FileSystemTools`,
`SmartWebFetchTool`, plus `BraveWebSearchTool` if a key is set. Per-agent
front-matter `tools`/`disallowedTools` then filter that pool.

### Discovering subagent references

```java
List<SubagentReference> refs = ClaudeSubagentReferences.fromRootDirectory("/subagents");
// also: fromRootDirectories(List), fromResources(Resource[]|List), fromResource(Resource)
```

Each `.md` file becomes one `SubagentReference` with kind `"CLAUDE"`.

---

## Remote sub-agents — `spring-ai-agent-utils-a2a`

Package: `org.springaicommunity.agent.subagent.a2a`
Coordinates: `org.springaicommunity:spring-ai-agent-utils-a2a`
Transitive deps: `io.github.a2asdk:a2a-java-sdk-client:0.3.3.Final` and
`io.github.a2asdk:a2a-java-sdk-client-transport-jsonrpc:0.3.3.Final`.

| Class | Role |
|---|---|
| `A2ASubagentDefinition` | Wraps an A2A `AgentCard`; `KIND = "A2A"` |
| `A2ASubagentResolver` | Fetches `<uri>/.well-known/agent-card.json` (path configurable) and produces an `A2ASubagentDefinition` |
| `A2ASubagentExecutor` | Builds an A2A JSON-RPC client, sends the prompt as a user message, captures `TaskEvent` artifact text into a `CompletableFuture<String>`, waits up to 60 s |

### Wiring an A2A sub-agent type

```java
SubagentType a2aType = new SubagentType(
    new A2ASubagentResolver(),
    new A2ASubagentExecutor());

SubagentReference ref = new SubagentReference(
    "https://agents.example.com/airbnb-helper",   // uri
    "A2A");                                        // kind
```

---

## Putting it all together

```java
@Configuration
class SubagentConfig {

  @Bean
  TaskRepository taskRepository() { return new DefaultTaskRepository(); }

  @Bean
  ToolCallback taskTool(TaskRepository repo,
                        ChatClient.Builder defaultBuilder) {
    SubagentType claude = ClaudeSubagentType.builder()
        .chatClientBuilder("default", defaultBuilder)
        .skillsDirectories(List.of("/skills"))
        .build();

    SubagentType a2a = new SubagentType(
        new A2ASubagentResolver(), new A2ASubagentExecutor());

    List<SubagentReference> refs = new ArrayList<>();
    refs.addAll(ClaudeSubagentReferences.fromRootDirectory("/subagents"));
    refs.add(new SubagentReference(
        "https://agents.example.com/airbnb-helper", "A2A"));

    return TaskTool.builder()
        .subagentReferences(refs)
        .subagentTypes(List.of(claude, a2a))
        .taskRepository(repo)
        .build();
  }

  @Bean
  ToolCallback taskOutputTool(TaskRepository repo) {
    return TaskOutputTool.builder().taskRepository(repo).build();
  }

  @Bean
  ChatClient orchestrator(ChatClient.Builder builder,
                          ToolCallback taskTool,
                          ToolCallback taskOutputTool) {
    return builder
        .defaultSystem("Delegate specialised work via the Task tool. " +
                       "Use TaskOutput to poll background tasks.")
        .defaultToolCallbacks(taskTool, taskOutputTool)
        .defaultAdvisors(ToolCallAdvisor.builder()
            .disableInternalConversationHistory()
            .build())
        .build();
  }
}
```

---

## When proposing sub-agent code

1. **Always pair `TaskTool` and `TaskOutputTool` with the same
   `TaskRepository` bean** — register both as `ToolCallback`s.
2. **`ClaudeSubagentType` requires a `default` chat-client-builder entry.**
   Additional named builders unlock `provider:model` overrides from
   sub-agent front-matter.
3. **A2A discovery hits `/.well-known/agent-card.json`.** If the remote
   agent uses a different path, pass it to `A2ASubagentResolver`'s
   constructor.
4. **A2A execution waits up to 60 s** for the first `TaskEvent` artifact. For
   long-running remote work, set `run_in_background=true` in the `TaskCall`
   and poll via `TaskOutputTool`.
5. **Front-matter `tools` / `disallowedTools` filter, they don't add** — only
   tools registered with `ClaudeSubagentType` (defaults + Brave if keyed) can
   be referenced.
6. **Built-in subagents are auto-registered** (`general-purpose`, `explore`,
   `plan`, `bash`). Don't redefine them in your `.md` files unless you intend
   to override.
7. **Shut down `DefaultTaskRepository`** on application stop (`@PreDestroy
   shutdown()`) to drain background tasks gracefully.
