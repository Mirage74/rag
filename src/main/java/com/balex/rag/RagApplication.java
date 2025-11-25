package com.balex.rag;

import com.balex.rag.advisors.expansion.ExpansionQueryAdvisor;
import com.balex.rag.advisors.rag.RagAdvisor;
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
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@RequiredArgsConstructor
public class RagApplication {

	private final ChatRepository chatRepository;
	private final VectorStore vectorStore;
	private final ChatModel chatModel;

	private static final PromptTemplate SYSTEM_PROMPT = new PromptTemplate(
			"""
                    Ты — Евгений Борисов, Java-разработчик и эксперт по Spring. Отвечай от первого лица, кратко и по делу.
                    
                    Вопрос может быть о СЛЕДСТВИИ факта из Context.
                    ВСЕГДА связывай: факт Context → вопрос.
                    
                    Нет связи, даже косвенной = "я не говорил об этом в докладах".
                    Есть связь = отвечай.
                    """
	);

	@Bean
	public ChatClient chatClient(ChatClient.Builder builder) {
		return builder
				.defaultAdvisors(
						ExpansionQueryAdvisor.builder(chatModel).order(0).build(),
						getHistoryAdvisor(1),
						SimpleLoggerAdvisor.builder().order(2).build(),
						RagAdvisor.build(vectorStore).order(3).build(),
						SimpleLoggerAdvisor.builder().order(4).build()
				)
				.defaultOptions(OllamaOptions.builder()
						.temperature(0.3).topP(0.7).topK(20).repeatPenalty(1.1)
						.build())
				.defaultSystem(SYSTEM_PROMPT.render())
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
