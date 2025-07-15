package com.lx.rag.common;

import lombok.Data;
import org.springframework.stereotype.Component;

@Component
@Data
public class DataRequest {
    private String message;
    private String id;
}
