package io.goudai.starter.kafka.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.TimeZone;

public final class JsonUtils {
	
	private static Logger logger = LoggerFactory.getLogger(JsonUtils.class);
	
	private static ObjectMapper mapper = new ObjectMapper();
	
	static {
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		mapper.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);
		mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
		mapper.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));
	}

	public static String toJson(Object object) {
		try {
			return mapper.writeValueAsString(object);
		} catch (Exception e) {
			logger.error("write to json string error:" + object, e);
			throw new RuntimeException(e);
		}
	}

	public static <T> T fromJson(String jsonString, Class<T> valueType) {
		Assert.notNull(valueType,"valueType is null ");
		if (StringUtils.isBlank(jsonString)) {
			return null;
		}
		try {
			return mapper.readValue(jsonString, valueType);
		} catch (Exception e) {
			logger.error("parse json string error:" + jsonString, e);
			throw new RuntimeException(e);
		}
	}

	public static <T> T fromJson(InputStream is, Class<T> valueType) {
		Assert.notNull(valueType,"valueType is null ");
		Assert.notNull(is,"inputStream is null");
		try {
			return mapper.readValue(is, valueType);
		} catch (Exception e) {
			logger.error("parse json string error", e);
			throw new RuntimeException(e);
		}
	}

	public static <T extends Collection<S>, S> T fromJson(String jsonString, Class<T> collectionType, Class<S> elementType) {
		Assert.notNull(collectionType,"collectionType is null");
		Assert.notNull(elementType,"elementType is null");
		if (StringUtils.isBlank(jsonString)) {
			return null;
		}
		try {
			return mapper.readValue(jsonString, mapper.getTypeFactory().constructCollectionType(collectionType, elementType));
		} catch (Exception e) {
			logger.error("parse json string error:" + jsonString, e);
			throw new RuntimeException(e);
		}
	}
	
	public static <T> T fromJson(String jsonString, TypeReference<T> typeReference) {
		Assert.notNull(typeReference,"typeReference is null");
		if (StringUtils.isEmpty(jsonString)) {
			return null;
		}
		try {
			return mapper.readValue(jsonString, typeReference);
		} catch (Exception e) {
			logger.error("parse json string error:" + jsonString, e);
			throw new RuntimeException(e);
		}
	}

}
