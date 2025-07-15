package com.lx.rag.controller;

import com.lx.rag.common.DataRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@RestController
@RequestMapping("/api")
@Slf4j
public class ChatController {

    private final ChatClient chatClient;

    // 存储会话数据的缓存（实际项目建议使用 Redis）
    private final Map<String, DataRequest> sessionCache = new ConcurrentHashMap<>();

    // 存储 SSE 连接的缓存
    private final Map<String, SseEmitter> emitterCache = new ConcurrentHashMap<>();

    public ChatController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    // 创建会话并返回会话 ID
    @PostMapping("/create-session")
    public Map<String, String> createSession(@RequestBody DataRequest request) {
        String sessionId = UUID.randomUUID().toString();
        sessionCache.put(sessionId, request);
        return Collections.singletonMap("sessionId", sessionId);
    }

    // SSE 端点：接收会话 ID，返回流式响应
    @GetMapping("/query")
    public SseEmitter query(@RequestParam("sessionId") String sessionId) {
        // 从缓存获取请求数据
        DataRequest request = sessionCache.get(sessionId);
        if (request == null) {
            throw new IllegalArgumentException("无效的会话 ID");
        }

        // 创建 SSE 发射器，超时时间设为 30 分钟
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        // 存储发射器，用于后续推送
        emitterCache.put(sessionId, emitter);

        // 设置完成、超时和错误处理
        emitter.onCompletion(() -> emitterCache.remove(sessionId));
        emitter.onTimeout(() -> emitterCache.remove(sessionId));
        emitter.onError(e -> emitterCache.remove(sessionId));

        try {
            // 记录日志
            log.info("用户输入:{}", request.getMessage());
            log.info("会话id:{}", sessionId);

            // 调用 AI 服务获取流式响应
            Flux<ChatResponse> stream = chatClient.prompt()
                    .user(request.getMessage())
                    .system(spec -> spec.param("time", LocalDateTime.now()))
                    .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, sessionId)
                            .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 30))
                    .functions("searchTrainInfo")
                    .stream()
                    .chatResponse();

            // 订阅流并发送数据到客户端
            stream.subscribe(
                    chatResponse -> {
                        String content = chatResponse.getResult().getOutput().getContent();
                        try {
                            // 发送符合 SSE 规范的数据
                            emitter.send(SseEmitter.event()
                                    .id(UUID.randomUUID().toString())
                                    .name("message")
                                    .data(content));
                        } catch (IOException e) {
                            log.error("发送 SSE 数据失败", e);
                            emitter.completeWithError(e);
                        }
                    },
                    error -> {
                        log.error("处理流数据失败", error);
                        try {
                            emitter.send(SseEmitter.event()
                                    .id(UUID.randomUUID().toString())
                                    .name("error")
                                    .data("处理请求时发生错误"));
                        } catch (IOException e) {
                            log.error("发送错误信息失败", e);
                        }
                        emitter.completeWithError(error);
                    },
                    () -> {
                        log.info("流数据处理完成");
                        emitter.complete();
                    }
            );

        } catch (Exception e) {
            log.error("处理 SSE 请求失败", e);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    // 新增：更新会话内容的接口
    @PostMapping("/update-session")
    public void updateSession(@RequestParam("s") String sessionId,
                              @RequestBody DataRequest request) {
        DataRequest existingRequest = sessionCache.get(sessionId);
        if (existingRequest != null) {
            // 合并新消息到现有会话（具体逻辑根据需求调整）
            existingRequest.setMessage(request.getMessage());
            // 可能需要维护对话历史数组
        } else {
            throw new IllegalArgumentException("无效的会话 ID");
        }
    }

    // 关闭会话的接口
    @PostMapping("/close-session")
    public void closeSession(@RequestParam("s") String sessionId) {
        SseEmitter emitter = emitterCache.get(sessionId);
        if (emitter != null) {
            emitter.complete();
            emitterCache.remove(sessionId);
        }
        sessionCache.remove(sessionId);
    }
}