package com.lx.rag.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@RestController
@RequestMapping("/ai")
@Slf4j
public class RagController {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public RagController(ChatClient chatClient, VectorStore vectorStore) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
    }


    @GetMapping("/rag")
    public String rag(String input) {
        return chatClient.prompt()
                .user(input)
                .advisors(new QuestionAnswerAdvisor(vectorStore))
                .call()
                .content();
    }

    @GetMapping(value = "/query", produces = "text/html;charset=UTF-8")
    public Flux<String> query(@RequestParam String message, @RequestParam String id) {
        log.info("用户输入:{}", message);
        if (id == null) {
            // 对会话的唯一标识
            id = UUID.randomUUID().toString();
        }
        log.info("会话id:{}", id);
        String finalId = id;
        Flux<ChatResponse> stream = chatClient.prompt()
                .user(message)
                .system(spec->spec.param("time", LocalDateTime.now()))
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, finalId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 30))
                .functions("searchTrainInfo")
                .stream()
                .chatResponse();
        return stream.map(chatResponse -> chatResponse.getResult().getOutput().getContent());
    }
}
