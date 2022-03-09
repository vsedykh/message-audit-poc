package com.epam.messageaudit.ingester;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KGroupedStream;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.KeyValueMapper;
import org.apache.kafka.streams.kstream.ValueMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SpanProcessor {

    private static String[] TAGS_LIST = new String[]{"source-system", "product-name", "product-id", "stage", "target-system"};
    private ObjectMapper om = new ObjectMapper();

    private static final Serde<String> STRING_SERDE = Serdes.String();
    private static final Serde<byte[]> BYTE_ARRAY_SERDE = Serdes.ByteArray();

    @Autowired
    void buildPipeline(StreamsBuilder streamsBuilder) {
        ValueMapper<byte[], Map<String, Object>> byteToMapVM = new ValueMapper<byte[], Map<String, Object>>() {
            @Override
            public Map<String, Object> apply(byte[] bytes) {
                try {
                    return om.readValue(bytes, Map.class);
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
        };

        ValueMapper<Map<String, Object>, byte[]> mapToByteVM = new ValueMapper<Map<String, Object>, byte[]>() {
            @Override
            public byte[] apply(Map<String, Object> json) {
                try {
                    return om.writeValueAsBytes(json);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
        };

        ValueMapper<MessageAudit, byte[]> pojoToByteVM = new ValueMapper<MessageAudit, byte[]>() {
            @Override
            public byte[] apply(MessageAudit pojo) {
                try {
                    return om.writeValueAsString(pojo).getBytes(StandardCharsets.UTF_8);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
        };

        ValueMapper<Map<String, Object>, MessageAudit> mapToPojo = new ValueMapper<Map<String, Object>, MessageAudit>() {
            @Override
            public MessageAudit apply(Map<String, Object> span) {
                MessageAudit ma = new MessageAudit();
                ma.setTraceID(encodeHexString(Base64.getDecoder().decode((String)span.get("traceId"))));
                List<Map<String, Object>> tags = (List<Map<String, Object>>)span.get("tags");
                tags.stream().forEach(tag -> {
                    String key = (String)tag.get("key");
                    String value = (String) tag.get("vStr");
                    System.out.println(key + "|" + value);
                    if (key.equalsIgnoreCase("product-id")) {
                        ma.setProductId(value);
                    } else if (key.equalsIgnoreCase("stage")) {
                        ma.setStatus(value);
                        if (value.equalsIgnoreCase("start")) {
                            ma.setStartTimestamp(Date.from(LocalDateTime.parse((String)span.get("startTime"), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSS]'Z'")).toInstant(
                                ZoneOffset.UTC)));
                        } else {
                            ma.setUpdateTimestamp(Date.from(LocalDateTime.parse((String)span.get("startTime"), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSS]'Z'")).toInstant(
                                ZoneOffset.UTC)));
                        }
                    } else if (key.equalsIgnoreCase("product-name")) {
                        ma.setProductName(value);
                    }  else if (key.equalsIgnoreCase("source-system")) {
                        ma.setSourceSystem(value);
                    }  else if (key.equalsIgnoreCase("target-system")) {
                        ma.setTargetSystem(value);
                    }
                });
                return ma;
            }
        };

        Serializer<MessageAudit> serializer = new Serializer<MessageAudit>() {
            @Override
            public byte[] serialize(String topic, MessageAudit data) {
                try {
                    return om.writeValueAsBytes(data);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
        };

        Deserializer<MessageAudit> deserializer = new Deserializer<MessageAudit>() {
            @Override
            public MessageAudit deserialize(String topic, byte[] data) {
                try {
                    return om.readValue(data, MessageAudit.class);
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
        };

        KGroupedStream<String, MessageAudit> messageStream = streamsBuilder
            .stream("jaeger", Consumed.with(STRING_SERDE, BYTE_ARRAY_SERDE))
            .mapValues(byteToMapVM)
            .filter((k, v) -> ((List)v.get("tags")).stream().map(m -> (String)((Map<String, String>)m).get("key")).anyMatch(key -> Arrays.asList(TAGS_LIST).contains(key)))
            .mapValues(mapToPojo)
            .peek((k, v) -> System.out.println("Before groupBy"))
//            .groupBy((k,v) -> v.getTraceID(), )
            .groupBy((k,v) -> v.getTraceID(), Grouped.with(STRING_SERDE, Serdes.serdeFrom(serializer, deserializer)))
//            .peek((k, v) -> System.out.println(v))
//            .mapValues(mapToByteVM);
        ;

        KTable<String, byte[]> aggregated = messageStream.reduce((o, n) -> {
                System.out.println("In reduce");
                if (n.getStatus() != null) o.setStatus(n.getStatus());
                if (o.getProductId() == null) o.setProductId(n.getProductId());
                if (o.getProductName() == null) o.setProductName(n.getProductName());
                if (o.getSourceSystem() == null) o.setSourceSystem(n.getSourceSystem());
                if (n.getTargetSystem() != null) {
                    if (o.getTargetState().isEmpty()) {
                        TargetState ts = new TargetState();
                        ts.setSystem(n.getTargetSystem());
                        ts.setState(n.getStatus());
                        o.getTargetState().add(ts);
                    } else {
                        o.getBySystem(n.getTargetSystem()).setState(n.getStatus());
                    }
                }
                if (o.getUpdateTimestamp() == null) {
                    o.setUpdateTimestamp(n.getUpdateTimestamp());
                } else if (o.getUpdateTimestamp() != null && n.getUpdateTimestamp() != null && o.getUpdateTimestamp().compareTo(n.getUpdateTimestamp()) < 0) {
                    o.setUpdateTimestamp(n.getUpdateTimestamp());
                }
                if (n.getError() != null) o.setError(n.getError());
                return o;
            })
            .mapValues(pojoToByteVM);

        aggregated.toStream().peek((k, v) -> System.out.println("Before sending")).to("message-audit");
    }


    private String encodeHexString(byte[] byteArray) {
        StringBuffer hexStringBuffer = new StringBuffer();
        for (int i = 0; i < byteArray.length; i++) {
            hexStringBuffer.append(byteToHex(byteArray[i]));
        }
        return hexStringBuffer.toString();
    }

    private String byteToHex(byte num) {
        char[] hexDigits = new char[2];
        hexDigits[0] = Character.forDigit((num >> 4) & 0xF, 16);
        hexDigits[1] = Character.forDigit((num & 0xF), 16);
        return new String(hexDigits);
    }
}
