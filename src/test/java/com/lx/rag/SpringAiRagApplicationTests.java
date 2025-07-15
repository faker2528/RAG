package com.lx.rag;

import com.lx.rag.service.TranService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SpringAiRagApplicationTests {

    @Autowired
    private TranService tranService;
    @Test
    void httpTest(){
        tranService.trainCrawl("重庆", "厦门", "2025-07-16");
    }


}
