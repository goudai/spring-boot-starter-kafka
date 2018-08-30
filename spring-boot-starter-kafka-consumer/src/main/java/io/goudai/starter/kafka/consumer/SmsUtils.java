package io.goudai.starter.kafka.consumer;

import io.goudai.starter.kafka.consumer.autoconfigure.KafkaConsumerAutoConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;
import java.util.Base64;

@Slf4j
public class SmsUtils {

    private final String URL = "http://sms-api.luosimao.com/v1/send.json";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private KafkaConsumerAutoConfiguration.KafkaConsumerProperties kafkaConsumerProperties;

    public void send(String format) {
        final String apiKey = kafkaConsumerProperties.getSmsApiKey();
        if (StringUtils.isEmpty(apiKey)) {
            log.info("luosimao 无配置 不进行报警信息发送 ：%s", format);
            return;
        }
        String authorization = "Basic " + Base64.getEncoder().encodeToString(("api:" + apiKey).getBytes(Charset.forName("UTF-8")));
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorization);
        headers.set("Content-Type", "application/x-www-form-urlencoded");

        for (String phone : kafkaConsumerProperties.getPhoneList()) {
            LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("mobile", phone);
            params.add("message", format);
            final String post = restTemplate.postForObject(URL, new HttpEntity<>(params, headers), String.class);
            log.info(post + "提交告警信息成功: {}", format);
        }
    }
}
