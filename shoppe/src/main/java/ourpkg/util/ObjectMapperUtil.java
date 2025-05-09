package ourpkg.util;

import java.util.HashMap;
import java.util.Map;

import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class ObjectMapperUtil {

	private static final ObjectMapper objectMapper = new ObjectMapper();

	@Named("mapToJson")
	public String mapToJson(Map<String, String> specPairs) {
		if (specPairs == null || specPairs.isEmpty()) {
			return "{}"; // 避免存入 null，改成空 JSON
		}
		try {
			return objectMapper.writeValueAsString(specPairs);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("JSON 轉換失敗", e);
		}
	}

	@Named("mapToMap")
	public Map<String, String> mapToMap(String specPairs) {
		if (specPairs == null || specPairs.isBlank()) {
			return new HashMap<>(); // 避免 null 轉換錯誤
		}
		try {
			return objectMapper.readValue(specPairs, new TypeReference<>() {
			});
		} catch (JsonProcessingException e) {
			throw new RuntimeException("JSON 解析失敗", e);
		}
	}
}
