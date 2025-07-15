package com.lx.rag.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@Slf4j
public class RagConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        log.info("加载ChatClient配置...");
        return builder
                .defaultSystem("Your name is Meya, and you serve as an intelligent train ticketing assistant, offering users precise and error-free responses. If you are uncertain or don't know, please reply clearly and refrain from fabricating or providing irrelevant answers.The current time is {time}.")
                .defaultAdvisors(new MessageChatMemoryAdvisor(new InMemoryChatMemory()))
                .build();

    }

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        log.info("加载VectorStore配置...");
        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();
        // 生成说明文档
        List<Document> documents = List.of(new Document("""
                产品说明:名称：Java开发语言
                产品描述：Java是一种面向对象开发语言。
                特性：
                1. 封装
                2. 继承
                3. 多态
                """));
        // 向量化存储
        store.add(documents);
        return store;
    }
}
