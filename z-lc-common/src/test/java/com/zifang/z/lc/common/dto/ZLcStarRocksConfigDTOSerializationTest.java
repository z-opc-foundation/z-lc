package com.zifang.z.lc.common.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcStarRocksConfigDTO 序列化/反序列化测试
 *
 * @author zifang
 */
class ZLcStarRocksConfigDTOSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldSerializeAndDeserialize() throws Exception {
        ZLcStarRocksConfigDTO original = new ZLcStarRocksConfigDTO();
        original.setHost("192.168.1.100");
        original.setPort(8030);
        original.setUser("root");
        original.setPassword("secret");
        original.setDb("analytics");
        original.setTable("events");

        String json = mapper.writeValueAsString(original);
        ZLcStarRocksConfigDTO deserialized = mapper.readValue(json, ZLcStarRocksConfigDTO.class);

        assertThat(deserialized.getHost()).isEqualTo("192.168.1.100");
        assertThat(deserialized.getPort()).isEqualTo(8030);
        assertThat(deserialized.getUser()).isEqualTo("root");
        assertThat(deserialized.getPassword()).isEqualTo("secret");
        assertThat(deserialized.getDb()).isEqualTo("analytics");
        assertThat(deserialized.getTable()).isEqualTo("events");
    }

    @Test
    void shouldSerializeWithPartialValues() throws Exception {
        ZLcStarRocksConfigDTO config = new ZLcStarRocksConfigDTO();
        config.setHost("localhost");

        String json = mapper.writeValueAsString(config);
        assertThat(json).contains("\"host\":\"localhost\"");
    }

    @Test
    void shouldDeserializeFromJson() throws Exception {
        String json = "{\"host\":\"10.0.0.1\",\"port\":9030,\"user\":\"admin\"}";
        ZLcStarRocksConfigDTO config = mapper.readValue(json, ZLcStarRocksConfigDTO.class);

        assertThat(config.getHost()).isEqualTo("10.0.0.1");
        assertThat(config.getPort()).isEqualTo(9030);
        assertThat(config.getUser()).isEqualTo("admin");
    }

    @Test
    void shouldDeserializePartialJson() throws Exception {
        String json = "{\"host\":\"localhost\"}";
        ZLcStarRocksConfigDTO config = mapper.readValue(json, ZLcStarRocksConfigDTO.class);

        assertThat(config.getHost()).isEqualTo("localhost");
        assertThat(config.getPort()).isNull();
        assertThat(config.getUser()).isNull();
    }
}
