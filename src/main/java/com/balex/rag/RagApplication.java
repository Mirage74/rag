package com.balex.rag;

import com.balex.rag.advisors.expansion.ExpansionQueryAdvisor;
import com.balex.rag.advisors.rag.RagAdvisor;
import com.balex.rag.config.RagDefaultsProperties;
import com.balex.rag.config.RagExpansionProperties;
import com.balex.rag.repo.ChatRepository;
import com.balex.rag.service.PostgresChatMemory;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@RequiredArgsConstructor
@EnableConfigurationProperties({RagDefaultsProperties.class, RagExpansionProperties.class})
public class RagApplication {

	private final ChatRepository chatRepository;
	private final VectorStore vectorStore;
	private final ChatModel chatModel;
	private final RagExpansionProperties expansionProperties;

	@Bean
	public ChatClient chatClient(
			ChatClient.Builder builder,
			@Value("${rag.rerank-fetch-multiplier}") int rerankFetchMultiplier,
			RagDefaultsProperties ragDefaults) {
		return builder
				.defaultAdvisors(
						getHistoryAdvisor(0),
						ExpansionQueryAdvisor.builder(chatModel, expansionProperties).order(1).build(),
						SimpleLoggerAdvisor.builder().order(2).build(),
						RagAdvisor.build(vectorStore)
								.rerankFetchMultiplier(rerankFetchMultiplier)
								.searchTopK(ragDefaults.searchTopK())
								.similarityThreshold(ragDefaults.similarityThreshold())
								.order(3).build(),
						SimpleLoggerAdvisor.builder().order(4).build()
				)
				.defaultOptions(OllamaOptions.builder()
						.temperature(ragDefaults.temperature())
						.repeatPenalty(ragDefaults.repeatPenalty())
						.build())
				.build();
	}

	private Advisor getHistoryAdvisor(int order) {
		return MessageChatMemoryAdvisor.builder(getChatMemory()).order(order).build();
	}

	private ChatMemory getChatMemory() {
		return PostgresChatMemory.builder()
				.maxMessages(8)
				.chatMemoryRepository(chatRepository)
				.build();
	}

	public static void main(String[] args) {
		SpringApplication.run(RagApplication.class, args);
	}

}
