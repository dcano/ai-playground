# Claude Code Skill: spring-ai-agent-utils

A reusable Claude Code skill providing authoritative reference material for
[`org.springaicommunity:spring-ai-agent-utils`](https://github.com/spring-ai-community/spring-ai-agent-utils).
Load this skill whenever a coding agent works on a Spring AI project that uses
this library.

## Contents

| File | What's in it |
|---|---|
| `SKILL.md` | Skill entry point: when to use, Maven coords, wiring patterns, canonical agent skeleton, common mistakes |
| `TOOLS_REFERENCE.md` | Full builder API for all 11 tools (FileSystemTools, ShellTools, GrepTool, GlobTool, ListDirectoryTool, SkillsTool, SmartWebFetchTool, BraveWebSearchTool, TodoWriteTool, AskUserQuestionTool, AutoMemoryTools) + `AutoMemoryToolsAdvisor` |
| `SUBAGENTS.md` | Sub-agent SPI (`SubagentDefinition` / `Resolver` / `Executor`), `TaskTool`, `TaskOutputTool`, `TaskRepository`, local Claude sub-agents (`ClaudeSubagentType`), and remote A2A sub-agents |

## Install (user-level, any project)

```bash
mkdir -p ~/.claude/skills/
cp -R .claude/skills/spring-ai-agent-utils ~/.claude/skills/
```

After installation, every Claude Code session — in any repo — will pick this
skill up. The skill's YAML front-matter `description` triggers it only when
the user's task actually involves the library, so it stays out of the way
otherwise.

## Install (project-level, this repo only)

The skill is already at `.claude/skills/spring-ai-agent-utils/`. Nothing to
do — Claude Code auto-loads project-level skills.

## Source of truth

All API details are extracted from the `main` branch of the upstream
repository at version **0.8.0-SNAPSHOT**. If you're on an older release
(e.g. `0.6.0` used by the published examples), most APIs still apply but
verify against your classpath.

- Upstream: https://github.com/spring-ai-community/spring-ai-agent-utils
- Docs site: https://spring-ai-community.github.io/spring-ai-agent-utils/0.8.0-SNAPSHOT/
