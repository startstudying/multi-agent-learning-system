# Skill: Spring AI Agent Tool

## When to Use

- Creating new Agent Tool functions
- Wiring Spring AI Tool calling
- Adding tools to agent orchestrator

## Pattern

```java
@Component
public class ExampleTool {

    private final ExampleService exampleService;

    @Tool(description = "Look up learner profile by user ID")
    public LearnerProfileDto getLearnerProfile(
            @ToolParam(description = "User ID") Long userId) {
        return exampleService.getProfile(userId);
    }
}
```

## Layering Rule

```
Agent → @Tool method → Service → Mapper/Repository
```

Never:

```java
// WRONG: Tool calling Mapper directly
@Tool
public User getUser(Long id) {
    return userMapper.selectById(id);
}
```

## Rules

- Tool description must be clear for model selection
- Tool params use `@ToolParam` with descriptions
- Tool delegates to Service layer
- Tool execution logged in agent trace
- Max tool round trips enforced at orchestrator level
- Validate tool output before returning to agent

## Registration

Tools registered in agent configuration / orchestrator setup.
Group related tools by domain (learning, assessment, rag).
