package com.lx.rag.common;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@Data
public class TrainQueryResult {
    // 使用@JSONField注解映射JSON中的下划线字段到Record的驼峰参数
    public record QueryInfo(
            @JSONField(name = "from_station") String fromStation,
            @JSONField(name = "to_station") String toStation,
            @JSONField(name = "date") String date,
            @JSONField(name = "timestamp") String timestamp
    ) {}

    public record TrainInfo(
            @JSONField(name = "train_number") String trainNo,
            @JSONField(name = "depart_station") String departStation,
            @JSONField(name = "arrive_station") String arrivalStation,
            @JSONField(name = "depart_time") String departTime,
            @JSONField(name = "arrive_time") String arriveTime,
            @JSONField(name = "is_bookable") boolean isBookable,
            @JSONField(name = "seats") List<Seat> seats
    ) {}

    public record Seat(
            @JSONField(name = "seat_name") String seatName,
            @JSONField(name = "seat_price") double seatPrice,
            @JSONField(name = "seat_bookable") boolean seatBookable,
            @JSONField(name = "seat_inventory") int seatInventory
    ) {}

    private QueryInfo queryInfo;
    private List<TrainInfo> trainInfoList;
}