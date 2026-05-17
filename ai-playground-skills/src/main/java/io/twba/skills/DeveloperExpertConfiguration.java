package io.twba.skills;

import java.io.IOException;

import org.springaicommunity.agent.tools.SkillsTool;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration
public class DeveloperExpertConfiguration {

    @Value("classpath:skills/developer-expert/SKILL.md")
    private Resource developerExpertSkill;

    @Bean
    ToolCallback developerExpertSkillTool() throws IOException {
        return SkillsTool.builder()
                .addSkillsResource(developerExpertSkill)
                .build();
    }

    @Bean
    ChatClient developerExpertChatClient(ChatClient.Builder builder,
            ToolCallback developerExpertSkillTool) {
        return builder
                .defaultSystem("""
                        You are an expert software developer assistant.
                        Always invoke the developer-expert skill to provide accurate,
                        comprehensive, and actionable guidance. When writing code, follow
                        the best practices defined in the skill: prefer immutability,
                        fail fast, keep methods small, and never leave unfinished stubs.
                        """)
                .defaultToolCallbacks(developerExpertSkillTool)
                .defaultAdvisors(ToolCallAdvisor.builder().build())
                .build();
    }
}
