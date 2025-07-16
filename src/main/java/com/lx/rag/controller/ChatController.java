package com.lx.rag.controller;

import com.lx.rag.common.DataRequest;
import com.lx.rag.mapper.SessionManager;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@RestController
@RequestMapping("/api")
@Slf4j
public class ChatController {

    private final ChatClient chatClient;
    private final SessionManager sessionManager;

    public ChatController(ChatClient chatClient, SessionManager sessionManager) {
        this.chatClient = chatClient;
        this.sessionManager = sessionManager;
    }

    /**
     * 创建会话（返回唯一sessionId）
     */
    @PostMapping("/create-session")
    public Map<String, String> createSession(@RequestBody DataRequest request) {
        log.info("收到创建会话请求: {}", request);
        validateRequest(request);

        String sessionId = sessionManager.createSession(request);
        log.info("创建新会话成功，sessionId={}", sessionId);

        return Collections.singletonMap("sessionId", sessionId);
    }

    /**
     * SSE流式响应接口
     */
    @GetMapping(value = "/query", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> query(@RequestParam("sessionId") String sessionId,
                              HttpServletResponse response) {
        log.info("收到查询请求，sessionId={}", sessionId);

        DataRequest request = sessionManager.getSession(sessionId)
                .orElseThrow(() -> {
                    log.warn("会话不存在，sessionId={}", sessionId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "会话不存在");
                });

        // 标记连接为活跃状态
        sessionManager.markConnectionActive(sessionId);

        return chatClient.prompt()
                .user(request.getMessage())
                .system(spec -> spec.param("time", LocalDateTime.now()))
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, sessionId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 30))
                .functions("searchTrainInfo")
                .stream()
                .chatResponse()
                .map(chatResponse -> formatSseMessage(chatResponse.getResult().getOutput().getContent()))
                .doOnCancel(() -> sessionManager.markConnectionInactive(sessionId))
                .doOnTerminate(() -> sessionManager.markConnectionInactive(sessionId));
    }

    /**
     * 更新会话内容
     */
    @PostMapping("/update-session")
    public void updateSession(@RequestParam("sessionId") String sessionId,
                              @RequestBody DataRequest request) {
        log.info("收到更新会话请求，sessionId={}", sessionId);
        validateRequest(request);

        sessionManager.updateSession(sessionId, request);
        log.info("会话内容更新成功，sessionId={}", sessionId);
    }

    /**
     * 关闭会话
     */
    @PostMapping("/close-session")
    public void closeSession(@RequestParam("sessionId") String sessionId) {
        log.info("收到关闭会话请求，sessionId={}", sessionId);

        sessionManager.closeSession(sessionId);
        log.info("会话关闭成功，sessionId={}", sessionId);
    }

    // 辅助方法：格式化SSE消息
    private String formatSseMessage(String content) {
        return "data: " + content.replace("\n", "\ndata: ") + "\n\n";
    }

    // 验证请求是否有效
    private void validateRequest(DataRequest request) {
        if (request == null || request.getMessage() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请求参数不能为空");
        }
    }
}