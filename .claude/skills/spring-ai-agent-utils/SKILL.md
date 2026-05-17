---
name: spring-ai-agent-utils
description: >
  Use when working on a Java/Spring AI project that integrates the
  spring-ai-agent-utils library (org.springaicommunity:spring-ai-agent-utils)
  or when the user asks how to build Spring AI agents with file-system,
  shell, web-fetch, web-search, grep/glob, skills, todo, memory,
  ask-user-question, or sub-agent (TaskTools / A2A) capabilities.
  Trigger on imports from org.springaicommunity.agent.* or mentions of
  SkillsTool, FileSystemTools, ShellTools, BraveWebSearchTool,
  SmartWebFetchTool, TodoWriteTool, AutoMemoryTools, AskUserQuestionTool,
  ToolCallAdvisor, SubagentDefinition / SubagentResolver / SubagentExecutor.
  Do NOT use for unrelated Spring or generic Java tasks.
---

# Spring AI Agent Utils

`spring-ai-agent-utils` is a Spring AI community library that reimplements
Claude Code's tool surface (file ops, shell, web, grep/glob, todo, memory,
skills, sub-agents) as Spring AI `ToolCallback`s, ready to attach to a
`ChatClient`.

**Authoritative source:** https://github.com/spring-ai-community/spring-ai-agent-utils
**Docs (snapshot):** https://spring-ai-community.github.io/spring-ai-agent-utils/0.8.0-SNAPSHOT/

## Core requirements

- Java **17+**
- Spring Boot **3.x or 4.x**
- Spring AI **2.0.0-M4+** (0.8.0-SNAPSHOT pins 2.0.0-M3)
- Maven **3.6+**

> **Compatibility gotcha:** the library is built against Spring AI 2.x. It is
> **not** compatible with Spring AI 1.x projects. If the host project pins
> 1.x, propose a standalone module rather than forcing a parent-pom upgrade.

## Maven coordinates

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.springaicommunity</groupId>
      <artifactId>spring-ai-agent-utils-bom</artifactId>
      <version>0.8.0-SNAPSHOT</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependencies>
  <dependency>
    <groupId>org.springaicommunity</groupId>
    <artifactId>spring-ai-agent-utils</artifactId>
  </dependency>
</dependencies>
```

Snapshot repositories required for `-SNAPSHOT` versions:

```xml
<repositories>
  <repository>
    <id>spring-snapshots</id>
    <url>https://repo.spring.io/snapshot</url>
    <releases><enabled>false</enabled></releases>
  </repository>
  <repository>
    <id>central-portal-snapshots</id>
    <url>https://central.sonatype.com/repository/maven-snapshots/</url>
    <releases><enabled>false</enabled></releases>
  </repository>
</repositories>
```

For sub-agents over A2A, also add `spring-ai-agent-utils-a2a`.

## Modules at a glance

| Module | Purpose |
|---|---|
| `spring-ai-agent-utils` | All tools, advisors, default `SkillsTool` |
| `spring-ai-agent-utils-common` | Sub-agent SPI (`SubagentDefinition`, `SubagentResolver`, `SubagentExecutor`, `SubagentType`) |
| `spring-ai-agent-utils-a2a` | Remote sub-agents over the A2A HTTP protocol |
| `spring-ai-agent-utils-bom` | Centralised version management |

## Wiring tools into a ChatClient

All tools live in package `org.springaicommunity.agent.tools`. They use the
**static-builder pattern**: `MyTool.builder()...build()`.

Two registration patterns exist:

1. **Annotation-based tools** (most tools) — wrap the instance with
   `ToolCallbacks.from(...)` and pass via `.defaultTools(...)`:
   ```java
   chatClientBuilder
       .defaultTools(
           ShellTools.builder().build(),
           FileSystemTools.builder().build())
       .build();
   ```
2. **Direct `ToolCallback`** (only `SkillsTool`) — pass via
   `.defaultToolCallbacks(...)`:
   ```java
   chatClientBuilder
       .defaultToolCallbacks(
           SkillsTool.builder().addSkillsDirectory("/skills").build())
       .build();
   ```

Always add `ToolCallAdvisor` so tool invocations are routed correctly:

```java
chatClientBuilder.defaultAdvisors(ToolCallAdvisor.builder().build());
```

## The 11 tools

| Tool | Header | One-line builder |
|---|---|---|
| `FileSystemTools` | `Read`/`Write`/`Edit` | `FileSystemTools.builder().build()` |
| `ShellTools` | `Bash`/`BashOutput`/`KillShell` | `ShellTools.builder().build()` |
| `GrepTool` | `Grep` | `GrepTool.builder().workingDirectory(p).build()` |
| `GlobTool` | `Glob` | `GlobTool.builder().workingDirectory(p).build()` |
| `ListDirectoryTool` | `ListDirectory` | `ListDirectoryTool.builder().workingDirectory(p).build()` |
| `SkillsTool` | `Skill` | `SkillsTool.builder().addSkillsDirectory(d).build()` |
| `SmartWebFetchTool` | `WebFetch` | `SmartWebFetchTool.builder(chatClient).build()` |
| `BraveWebSearchTool` | `WebSearch` | `BraveWebSearchTool.builder(apiKey).resultCount(15).build()` |
| `TodoWriteTool` | `TodoWrite` | `TodoWriteTool.builder().todoEventHandler(h).build()` |
| `AskUserQuestionTool` | `AskUserQuestionTool` | `AskUserQuestionTool.builder().questionHandler(h).build()` |
| `AutoMemoryTools` | 6 `Memory*` tools | `AutoMemoryTools.builder().memoriesDir(p).build()` |

Full API surface for each tool — including builder methods, defaults, input
records, security behaviour, and example calls — is in
**[`TOOLS_REFERENCE.md`](TOOLS_REFERENCE.md)**. Load that file whenever the
user needs detail beyond the one-liner above.

## Skills (the `SkillsTool` system)

A *Skill* is a Markdown file named **`SKILL.md`** containing YAML front-matter
plus instructions. `SkillsTool` exposes a single tool (`Skill`) that the model
calls by name; the tool returns the skill's content as XML context.

Minimal SKILL.md:

```markdown
---
name: my-domain-expert
description: Use when the user asks about <X>. Provides <Y>.
---

# My Domain Expert

(instructions, references, examples...)
```

Load skills two ways:

```java
// From Spring Resources (classpath)
@Value("classpath:skills/my-domain-expert/SKILL.md") Resource skill;
SkillsTool.builder().addSkillsResource(skill).build();

// From a filesystem directory containing one subfolder per skill
SkillsTool.builder().addSkillsDirectory("/abs/path/skills").build();
```

`build()` throws if zero skills are configured.

## Sub-agents (TaskTools + A2A)

The sub-agent system lets the primary agent delegate to specialised agents,
either **local** (defined in Markdown) or **remote** (via the A2A protocol).
See **[`SUBAGENTS.md`](SUBAGENTS.md)** for the SPI, configuration, and how
the dispatcher (`TaskTool`) works.

## Advisors

- `ToolCallAdvisor` — required for tool-call routing. Always add it.
- Project-specific logging advisors (see the `code-agent-demo` for the
  `MyLoggingAdvisor` pattern with `showSystemMessage`/`showAvailableTools`
  toggles).

## Canonical agent skeleton

```java
@Configuration
class AgentConfig {

  @Bean
  ChatClient agent(ChatClient.Builder builder,
                   @Value("${brave.api.key}") String braveKey) {
    var fetchClient = builder.clone().build();    // separate ChatClient for fetch summarisation
    return builder
        .defaultSystem("Use available tools and skills to answer.")
        .defaultToolCallbacks(
            SkillsTool.builder().addSkillsDirectory("/skills").build())
        .defaultTools(
            FileSystemTools.builder().build(),
            ShellTools.builder().build(),
            GrepTool.builder().workingDirectory("/repo").build(),
            GlobTool.builder().workingDirectory("/repo").build(),
            SmartWebFetchTool.builder(fetchClient).build(),
            BraveWebSearchTool.builder(braveKey).resultCount(15).build())
        .defaultAdvisors(ToolCallAdvisor.builder().build())
        .build();
  }
}
```

## When proposing code

1. **Verify the host project's Spring AI version first.** If it's 1.x, do not
   add `spring-ai-agent-utils` to existing modules — create a separate module
   pinned to Spring AI 2.x. Explain the trade-off to the user.
2. **Always include `ToolCallAdvisor`** — without it tool calls don't route.
3. **Use `defaultToolCallbacks` only for `SkillsTool`**; everything else goes
   through `defaultTools(...)`.
4. **Don't fabricate builder methods.** Stick to the API in
   `TOOLS_REFERENCE.md`; if uncertain, instruct the user to check the version
   they have on classpath.
5. **Secrets via env vars / properties**, never hard-coded
   (`BRAVE_API_KEY`, etc.).
6. **`SmartWebFetchTool` requires its own `ChatClient`** — clone the builder so
   you don't recurse the agent's own client through the fetch summariser.
7. **For snapshot versions**, ensure both the Spring Snapshots and Central
   Portal Snapshots repositories are declared.

## Common mistakes to flag

- Importing tool classes from the wrong package (always
  `org.springaicommunity.agent.tools`).
- Passing tool instances directly to `defaultToolCallbacks(...)` —
  annotation-based tools must be wrapped with `ToolCallbacks.from(...)` or
  passed via `.defaultTools(...)`.
- Forgetting to call `.build()` on the builder.
- Configuring `SkillsTool` with zero resources/directories — `build()` will
  assert.
- Path-traversal in `AutoMemoryTools` paths — absolute paths and `..` are
  rejected with `SecurityException`; always use relative paths.
- Using Spring AI 1.x APIs (`ChatClient.create(...)`, old advisor signatures)
  in a 2.x project.
