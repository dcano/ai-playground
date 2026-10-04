# spring-ai-agent-utils — Tools API Reference

All tools live in package **`org.springaicommunity.agent.tools`** and follow
the static-builder pattern. Defaults shown reflect the 0.8.0-SNAPSHOT source
on `main`.

> Registration recap:
> - `SkillsTool.build()` → returns `ToolCallback` → use `.defaultToolCallbacks(...)`.
> - All other `build()` methods → return a POJO with `@Tool` methods → wrap
>   with `ToolCallbacks.from(...)` or pass via `.defaultTools(...)`.

---

## 1. FileSystemTools — `Read`, `Write`, `Edit`

```java
FileSystemTools.builder().build();
```

No configurable fields. Tool methods:

| Tool | Signature | Defaults |
|---|---|---|
| `read` | `read(String filePath, Integer offset, Integer limit)` | `offset=1`, `limit=2000`; lines >2000 chars truncated; `cat -n` output |
| `write` | `write(String filePath, String content)` | Overwrites; creates parent dirs |
| `edit` | `edit(String filePath, String old_string, String new_string, Boolean replace_all)` | Exact-match; fails if `old_string` not unique unless `replace_all=true` |

---

## 2. ShellTools — `Bash`, `BashOutput`, `KillShell`

```java
ShellTools.builder().build();
```

Static `ConcurrentHashMap<String, BackgroundProcess>` tracks background shells.

| Tool | Signature | Notes |
|---|---|---|
| `bash` | `bash(String command, Long timeout, String description, Boolean runInBackground)` | Default timeout 120 000 ms (max 600 000); output truncated at 30 000 chars; returns `bash_id: shell_<ts>\n\n<output>` |
| `bashOutput` | `bashOutput(String bash_id, String filter)` | `filter` is a regex applied to new lines |
| `killShell` | `killShell(String bash_id)` | — |

---

## 3. GrepTool — `Grep` (pure-Java)

```java
GrepTool.builder()
    .workingDirectory("/repo")     // Path or String, default = JVM cwd
    .maxDepth(20)                  // default 100
    .maxLineLength(10_000)         // default 10 000
    .maxOutputLength(100_000)      // default 100 000
    .build();
```

Tool method:

```java
grep(String pattern, String path, String glob, OutputMode outputMode,
     Integer contextBefore, Integer contextAfter, Integer context,
     Boolean showLineNumbers, Boolean caseInsensitive, String type,
     Integer headLimit, Integer offset, Boolean multiline)
```

- `enum OutputMode { files_with_matches, count, content }` (default `files_with_matches`)
- `showLineNumbers` default `true`
- Built-in `type` map: `java, js, ts, py, rust, go, cpp, c, rb, php, cs,
  xml, json, yaml, md, txt, sh`
- Auto-ignored dirs: `.git, node_modules, target, build, .idea, .vscode,
  dist, __pycache__`

---

## 4. GlobTool — `Glob` (pure-Java)

```java
GlobTool.builder()
    .workingDirectory(Path.of("/repo"))
    .maxDepth(100)
    .maxResults(500)               // default 1000
    .build();
```

Tool method: `glob(String pattern, String path)`. Patterns lacking `**/` get
it auto-prepended. Results sorted by mtime (newest first). Same ignored
dirs as `GrepTool`.

---

## 5. ListDirectoryTool — `ListDirectory`

```java
ListDirectoryTool.builder().workingDirectory("/repo").build();
```

Tool method: `listDirectory(String path, Integer depth, Integer limit)` —
defaults `depth=1`, `limit=50`. Directories first, files alphabetically.
Extra ignored dirs vs grep/glob: `.gradle, .mvn`.

---

## 6. SkillsTool — `Skill` (returns `ToolCallback`)

```java
ToolCallback skills = SkillsTool.builder()
    .addSkillsDirectory("/abs/path/skills")        // or String, or List<String>
    .addSkillsResource(classpathResource)          // or List<Resource>
    .toolDescriptionTemplate("Use a skill. %s")    // optional override; must contain %s
    .build();                                       // asserts ≥1 skill
```

Input record: `SkillsInput(String command)` — `command` is the skill name from
front-matter.

Skill record: `Skill(String basePath, Map<String,Object> frontMatter,
String content)` with `name()` and `toXml()`.

`build()` constructs a `FunctionToolCallback.builder("Skill", fn)
.inputType(SkillsInput.class)`.

**SKILL.md format:**

```markdown
---
name: my-skill
description: Use when the user asks about <X>. Provides <Y>.
---

# My Skill
(content)
```

---

## 7. SmartWebFetchTool — `WebFetch` (`AutoCloseable`)

Requires a `ChatClient` for summarisation. Fetch → HTML → Markdown (Flexmark)
→ summarise. 15-min in-memory cache keyed by URL + prompt hash.

```java
SmartWebFetchTool.builder(chatClient)               // required, non-null
    .maxContentLength(200_000)                      // default 100 000
    .domainSafetyCheck(true)                        // default true
    .maxCacheSize(200)                              // default 100
    .failOpenOnSafetyCheckError(true)               // default true
    .maxRetries(3)                                  // default 2 (exp backoff on 5xx)
    .build();
```

Tool method: `webFetch(String url, String prompt)`.

Domain safety check calls `https://claude.ai/api/web/domain_info` —
disable with `.domainSafetyCheck(false)` if outbound is restricted.

> **Pattern:** create a *separate* `ChatClient` for the fetch tool (clone the
> builder) to avoid feeding the agent's own client back into the summariser.

---

## 8. BraveWebSearchTool — `WebSearch`

```java
BraveWebSearchTool.builder(System.getenv("BRAVE_API_KEY"))   // required, non-blank
    .resultCount(15)                                          // default 10, must be > 0
    .build();
```

- Endpoint: `https://api.search.brave.com/res/v1/web/search`
- Header: `X-Subscription-Token`
- Tool method: `webSearch(String query, List<String> allowedDomains,
  List<String> blockedDomains)` — domain filtering is client-side
- Returns JSON-serialised `List<SearchResult>` where `SearchResult(String
  title, String url, String description)`

---

## 9. TodoWriteTool — `TodoWrite`

Validates structured todo lists; enforces exactly **one** `in_progress` item.

```java
TodoWriteTool.builder()
    .todoEventHandler(todos -> uiBus.publish(todos))   // default = logger.debug
    .build();
```

```java
@FunctionalInterface
interface TodoEventHandler { void handle(Todos todos); }

record Todos(List<TodoItem> todos) {
    record TodoItem(String content, Status status, String activeForm) {}
    enum Status { pending, in_progress, completed }
}
```

Throws `IllegalArgumentException` on: null items, blank `content`/`activeForm`,
or more than one `in_progress`.

---

## 10. AskUserQuestionTool — `AskUserQuestionTool`

```java
AskUserQuestionTool.builder()
    .questionHandler(questions -> mapOfAnswers)     // required
    .answersValidation(true)                        // default true
    .build();
```

```java
@FunctionalInterface
interface QuestionHandler {
    Map<String, String> handle(List<Question> questions);
}

record Question(String question, String header, List<Option> options, Boolean multiSelect) {
    record Option(String label, String description) {}
}
```

- Compact ctor warns if `header.length() > 12`, if `options.size()` not in `2..4`.
- `multiSelect` defaults to `false`.
- Tool input is 1–4 `Question`s; throws nested `InvalidUserAnswerException`
  on validation failure.

---

## 11. AutoMemoryTools — 6 tools: `MemoryView`, `MemoryCreate`, `MemoryStrReplace`, `MemoryInsert`, `MemoryDelete`, `MemoryRename`

File-based memory store with **path-traversal protection** (`resolveSafePath`).

```java
AutoMemoryTools.builder()
    .memoriesDir("/var/agent/memories")          // default Paths.get("/memories")
    .build();                                     // calls Files.createDirectories
```

- Absolute paths and `..` rejected → `SecurityException`.
- All paths are relative to the memories root.
- Convention: keep an index file `MEMORY.md` at the root.
- Memory files should carry YAML front-matter with `name`, `description`,
  `type ∈ {user, feedback, project, reference}`.

| Tool | Signature | Notes |
|---|---|---|
| `memoryView` | `(String path, String viewRange)` | `viewRange="start,end"`; dirs 2 levels deep with sizes |
| `memoryCreate` | `(String path, String fileText)` | Fails if exists; creates parents |
| `memoryStrReplace` | `(String path, String oldStr, String newStr)` | `oldStr` must appear exactly once |
| `memoryInsert` | `(String path, Integer insertLine, String insertText)` | `0` = before first line |
| `memoryDelete` | `(String path)` | Recursive for dirs; root is protected |
| `memoryRename` | `(String oldPath, String newPath)` | Dest must not exist |

---

## Cross-cutting facts

- `GrepTool`, `GlobTool`, `ListDirectoryTool` all share a `workingDirectory`
  builder method overloaded with `Path` and `String`.
- `SmartWebFetchTool` and `BraveWebSearchTool` require a constructor argument
  in their `builder(...)` (chat client / API key respectively).
- `SkillsTool.build()` is the only one that returns `ToolCallback` directly.
- `AutoMemoryTools` and the path-traversal protection mean directory mounts
  in containers should be the *memories root*, never the host root.
- `GrepTool` has a deprecated public no-arg constructor; always use the builder.

---

## Library-provided advisor

`org.springaicommunity.agent.advisors.AutoMemoryToolsAdvisor`
**implements** `BaseChatMemoryAdvisor`. It's the **only** advisor inside
this library — `ToolCallAdvisor` and `MessageChatMemoryAdvisor` come from
Spring AI core.

```java
AutoMemoryToolsAdvisor.builder()
    .memoriesDirectory(Paths.get("/memories"))
    .memorySystemPromptResource(systemPromptResource)
    .memoryConsolidationTrigger((req, lastConsolidatedAt) -> /* boolean */ true)
    .order(Ordered.HIGHEST_PRECEDENCE + 200)        // default
    .build();
```

`before()` injects the memory system prompt and appends the memory tool
callbacks to the chat options. `after()` is a no-op — persistence happens
during the model call. Pair with `AutoMemoryTools` registered as a tool.

---

## Quick wiring template

```java
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor; // Spring AI core

ChatClient agent = chatClientBuilder
    .defaultSystem(systemPrompt)
    .defaultToolCallbacks(
        SkillsTool.builder().addSkillsDirectory(skillsDir).build())
    .defaultTools(
        FileSystemTools.builder().build(),
        ShellTools.builder().build(),
        GrepTool.builder().workingDirectory(repoDir).build(),
        GlobTool.builder().workingDirectory(repoDir).build(),
        ListDirectoryTool.builder().workingDirectory(repoDir).build(),
        SmartWebFetchTool.builder(fetchClient).build(),
        BraveWebSearchTool.builder(braveKey).resultCount(15).build(),
        TodoWriteTool.builder().todoEventHandler(uiHandler).build(),
        AskUserQuestionTool.builder().questionHandler(qHandler).build(),
        AutoMemoryTools.builder().memoriesDir(memDir).build())
    .defaultAdvisors(ToolCallAdvisor.builder()
        .disableInternalConversationHistory().build())
    .build();
```
