package io.twba.aiplayground;

import org.springframework.ai.model.google.genai.autoconfigure.chat.GoogleGenAiChatAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@EnableAutoConfiguration(exclude = {GoogleGenAiChatAutoConfiguration.class})
@Profile( "ollama")
public class OllamaAiModelConfiguration {
}
