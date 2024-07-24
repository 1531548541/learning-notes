package com.example.calcite.query;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.Deserializer;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

public class MyKafkaConsumer extends KafkaConsumer {

    public MyKafkaConsumer(Map<String, Object> configs) {
        super(configs);
    }

    @Override
    public ConsumerRecords poll(Duration timeout) {
        return super.poll(timeout);
    }
}
