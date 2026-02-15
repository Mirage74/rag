package com.balex.rag.advisors.expansion;

import com.balex.rag.config.RagExpansionProperties;
import lombok.Builder;
import lombok.Getter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.ollama.api.OllamaOptions;

import java.util.Map;

@Builder
public class ExpansionQueryAdvisor implements BaseAdvisor {


    private static final PromptTemplate template = PromptTemplate.builder()
            .template("""
            Expand the search query by adding relevant terms.
            
            Rules:
            - Keep all original words
            - Add up to 5 specific terms
            - Output ONLY the expanded query, nothing else
            - No explanations, no formatting, no quotes, no bullet points
            
            Examples:
            Question: what is spring
            Query: what is spring framework Java dependency injection
            
            Question: how to configure security
            Query: how to configure security Spring Security authentication authorization filter chain
            
            Question: {question}
            Query:
            """).build();


    public static final String ENRICHED_QUESTION = "ENRICHED_QUESTION";
    public static final String ORIGINAL_QUESTION = "ORIGINAL_QUESTION";
    public static final String EXPANSION_RATIO = "EXPANSION_RATIO";

    private ChatClient chatClient;

    public static ExpansionQueryAdvisorBuilder builder(ChatModel chatModel, RagExpansionProperties props) {
        return new ExpansionQueryAdvisorBuilder().chatClient(ChatClient.builder(chatModel)
                .defaultOptions(OllamaOptions.builder()
                        .temperature(props.temperature())
                        .topK(props.topK())
                        .topP(props.topP())
                        .repeatPenalty(props.repeatPenalty())
                        .build())
                .build());
    }

    @Getter
    private final int order;

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {

        String userQuestion = chatClientRequest.prompt().getUserMessage().getText();
        String enrichedQuestion = chatClient
                .prompt()
                .user(template.render(Map.of("question", userQuestion)))
                .call()
                .content();

        double ratio = enrichedQuestion.length() / (double) userQuestion.length();

        return chatClientRequest.mutate()
                .context(ORIGINAL_QUESTION, userQuestion)
                .context(ENRICHED_QUESTION, enrichedQuestion)
                .context(EXPANSION_RATIO, ratio)
                .build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {


        return chatClientResponse;
    }

}
