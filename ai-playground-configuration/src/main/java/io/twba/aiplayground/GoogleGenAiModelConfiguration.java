package io.twba.aiplayground;

import org.springframework.ai.model.ollama.autoconfigure.OllamaChatAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@EnableAutoConfiguration(exclude = {OllamaChatAutoConfiguration.class})
@Profile( "google_genai")
public class GoogleGenAiModelConfiguration {
}
