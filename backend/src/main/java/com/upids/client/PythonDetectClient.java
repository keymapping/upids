package com.upids.client;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Python 识别服务客户端
 */
@Slf4j
@Component
public class PythonDetectClient {

    @Value("${upids.python.base-url}")
    private String baseUrl;

    @Value("${upids.python.connect-timeout}")
    private int connectTimeout;

    @Value("${upids.python.read-timeout}")
    private int readTimeout;

    private RestClient restClient;

    @jakarta.annotation.PostConstruct
    public void init() {
        this.restClient = RestClient.builder()
                .requestFactory(new org.springframework.http.client.SimpleClientHttpRequestFactory() {{
                    setConnectTimeout(connectTimeout);
                    setReadTimeout(readTimeout);
                }})
                .build();
    }

    /**
     * 调用 Python 识别服务
     * POST /detect，超时10s
     */
    public DetectResponse detect(String imagePath, String pipelineId, Long recordId) {
        try {
            log.info("Calling Python detect service for image: {}", imagePath);

            DetectRequest request = new DetectRequest();
            request.setImagePath(imagePath);
            request.setPipelineId(pipelineId);
            request.setRecordId(recordId);

            DetectResponse response = restClient.post()
                    .uri(baseUrl + "/detect")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(request)
                    .retrieve()
                    .body(DetectResponse.class);

            log.info("Python detect response: {}", response);
            return response;
        } catch (Exception e) {
            log.error("Failed to call Python detect service: {}", e.getMessage());
            throw new RuntimeException("Python service unavailable: " + e.getMessage());
        }
    }

    /**
     * 检查 Python 服务健康状态
     */
    public boolean healthCheck() {
        try {
            String response = restClient.get()
                    .uri(baseUrl + "/health")
                    .retrieve()
                    .body(String.class);
            log.info("Python service health: {}", response);
            return true;
        } catch (Exception e) {
            log.warn("Python service health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 识别请求
     */
    @Data
    public static class DetectRequest {
        private String imagePath;
        private String pipelineId;
        private Long recordId;
    }

    /**
     * 识别响应 - 包含缺陷类型、置信度、严重等级、边框、来源
     */
    @Data
    public static class DetectResponse {
        private boolean success;
        private String defectType;
        private BigDecimal confidenceScore;
        private Integer severityLevel;
        private String bbox;
        private String source;
        private String result;
        private Double confidence;
        private java.util.List<java.util.Map<String, Object>> defects;
    }
}
