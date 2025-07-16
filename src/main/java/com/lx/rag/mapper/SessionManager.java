package com.lx.rag.mapper;

import com.lx.rag.common.DataRequest;
import com.lx.rag.exception.SessionNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class SessionManager {

    // 存储会话数据的缓存，添加过期机制可以考虑使用Guava Cache或Caffeine
    private final Map<String, DataRequest> sessionCache = new ConcurrentHashMap<>();

    // 存储活跃连接
    private final Map<String, AtomicBoolean> activeConnections = new ConcurrentHashMap<>();

    public String createSession(DataRequest request) {
        String sessionId = UUID.randomUUID().toString();
        sessionCache.put(sessionId, request);
        return sessionId;
    }

    public Optional<DataRequest> getSession(String sessionId) {
        return Optional.ofNullable(sessionCache.get(sessionId));
    }

    public void updateSession(String sessionId, DataRequest request) {
        if (!sessionCache.containsKey(sessionId)) {
            throw new SessionNotFoundException("会话不存在，无法更新: " + sessionId);
        }
        sessionCache.put(sessionId, request);
    }

    public void closeSession(String sessionId) {
        sessionCache.remove(sessionId);
        activeConnections.remove(sessionId);
    }

    public void markConnectionActive(String sessionId) {
        activeConnections.computeIfAbsent(sessionId, k -> new AtomicBoolean()).set(true);
    }

    public void markConnectionInactive(String sessionId) {
        AtomicBoolean active = activeConnections.get(sessionId);
        if (active != null) {
            active.set(false);
        }
    }

    public boolean isConnectionActive(String sessionId) {
        AtomicBoolean active = activeConnections.get(sessionId);
        return active != null && active.get();
    }
}