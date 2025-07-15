package com.lx.rag.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.lx.rag.common.TrainQueryResult;
import com.lx.rag.common.TrainRequest;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Configuration
@Slf4j
public class TranService {

    @Bean
    @Description("根据出发地和目的地以及出发日期来查询火车或者动车信息")
    public Function<TrainRequest, TrainQueryResult> searchTrainInfo() {
        return trainRequest -> trainCrawl(trainRequest.getFrom(), trainRequest.getTo(), trainRequest.getDate());
    }

    public TrainQueryResult trainCrawl(String from, String to, String date) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();

        String url = "http://127.0.0.1:5000/search/train";
        HttpUrl httpUrl = Objects.requireNonNull(HttpUrl.parse(url))
                .newBuilder()
                .addQueryParameter("from", from)
                .addQueryParameter("to", to)
                .addQueryParameter("date", date)
                .build();

        Request request = new Request.Builder()
                .url(httpUrl)
                .addHeader("Content-Type", "application/json")
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            log.info("正在请求url:{}", httpUrl);
            if (response.isSuccessful() && response.body() != null) {
                String dataStr = response.body().string();
                log.info("接口返回数据:{}", dataStr);

                // 1. 解析最外层JSON
                JSONObject rootObj = JSONObject.parseObject(dataStr);
                if (rootObj.getInteger("code") != 0) {
                    log.error("接口返回错误:{}", rootObj.getString("message"));
                    return null;
                }

                // 2. 获取核心数据字段"data"
                JSONObject dataObj = rootObj.getJSONObject("data");
                if (dataObj == null) {
                    log.error("接口返回数据中无data字段");
                    return null;
                }

                // 3. 解析query_info
                TrainQueryResult.QueryInfo queryInfo = dataObj.getObject("query_info", TrainQueryResult.QueryInfo.class);

                // 4. 解析trains数组 (简化嵌套对象处理)
                JSONArray trainsArray = dataObj.getJSONArray("trains");
                List<TrainQueryResult.TrainInfo> trainInfoList = new ArrayList<>();
                if (trainsArray != null) {
                    // Fastjson2中JSONArray的toJavaList方法用于转换为List
                    trainInfoList = trainsArray.toJavaList(TrainQueryResult.TrainInfo.class);
                }

                // 5. 组装最终结果对象
                TrainQueryResult result = new TrainQueryResult();
                result.setQueryInfo(queryInfo);
                result.setTrainInfoList(trainInfoList);

                return result;
            } else {
                log.error("请求失败！url:{}, 响应码:{}", httpUrl, response.code());
                return null;
            }
        } catch (Exception e) {
            log.error("请求发送失败！url:{}", httpUrl, e);
            throw new RuntimeException("查询火车信息失败", e);
        }
    }
}