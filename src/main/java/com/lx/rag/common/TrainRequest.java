package com.lx.rag.common;

import lombok.Data;
import org.springframework.stereotype.Component;

@Data
@Component
public class TrainRequest {
    private String from;
    private String to;
    private String date;
}
