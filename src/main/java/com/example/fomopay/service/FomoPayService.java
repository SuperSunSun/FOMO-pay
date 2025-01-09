package com.example.fomopay.service;

import com.example.fomopay.util.FomoPayUtil;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * FOMO Pay 支付服务实现类
 * <p>
 * 该类负责处理与FOMO Pay支付网关的所有交互，包括：
 * 1. 支付状态查询
 * 2. 退款处理
 * 3. 支付订单创建
 * 4. 支付集成测试
 */
@Service
public class FomoPayService {

    /**
     * 定时任务调度器，用于处理定期任务
     */
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

//    @PostConstruct
//    public void init() {
//        scheduler.scheduleAtFixedRate(this::checkPendingPayments, 0, 3, TimeUnit.SECONDS);
//    }

    /**
     * 服务销毁时清理资源
     */
    @PreDestroy
    public void cleanup() {
        scheduler.shutdown();
    }


    @Autowired
    private FomoPayUtil fomoPayUtil;

    /**
     * 商户ID
     */
    private static final String MID = "110000000000849";
    /**
     * 终端ID
     */
    private static final String TID = "10000007";
    /**
     * 密钥ID
     */
    private static final String KEY_ID = "a5142d28-7a40-4f39-b22a-1c26287d8aff";
    /**
     * FOMO Pay API 地址
     */
    private static final String API_URL = "https://pos.fomopay.net/rpc";

    /**
     * 计算位图
     *
     * @param fields 需要包含在bitmap中的字段编号数组
     * @return 计算后的16进制位图字符串
     */
    private String calculateBitmap(int[] fields) {
        // Create primary and secondary bitmaps
        boolean[] primary = new boolean[64];
        boolean[] secondary = new boolean[64];

        // Set bits for each field
        for (int field : fields) {
            if (field <= 64) {
                primary[field - 1] = true;
            } else {
                secondary[field - 65] = true;
                primary[0] = true; // Set first bit to indicate secondary bitmap exists
            }
        }

        // Convert primary bitmap to hex
        StringBuilder primaryHex = new StringBuilder();
        for (int i = 0; i < 64; i += 4) {
            int nibble = 0;
            for (int j = 0; j < 4; j++) {
                if (primary[i + j]) {
                    nibble |= (1 << (3 - j));
                }
            }
            primaryHex.append(Integer.toHexString(nibble));
        }

        // Convert secondary bitmap to hex
        StringBuilder secondaryHex = new StringBuilder();
        for (int i = 0; i < 64; i += 4) {
            int nibble = 0;
            for (int j = 0; j < 4; j++) {
                if (secondary[i + j]) {
                    nibble |= (1 << (3 - j));
                }
            }
            secondaryHex.append(Integer.toHexString(nibble));
        }

        // Combine primary and secondary bitmaps
        return primaryHex.toString() + secondaryHex.toString();
    }

    /**
     * 将16进制字符串转换为可读文本
     *
     * @param hex 16进制字符串
     * @return 转换后的可读文本
     */
    private String hexToString(String hex) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hex.length(); i += 2) {
            String str = hex.substring(i, i + 2);
            sb.append((char) Integer.parseInt(str, 16));
        }
        return sb.toString();
    }

    /**
     * 获取查询请求的HTTP头信息
     *
     * @return 包含认证信息的HTTP头Map
     */
    private static Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Authentication-Version", "1.1");
        headers.put("X-Authentication-Method", "SHA256WithRSA");
        headers.put("X-Authentication-KeyId", "your_key_id");
        headers.put("X-Authentication-Nonce", "your_nonce");
        headers.put("X-Authentication-Timestamp", "your_timestamp");
        headers.put("X-Authentication-Sign", "your_signature");
        return headers;
    }

    /**
     * 生成查询请求体
     *
     * @param stan 系统跟踪审计号
     * @return 查询请求的JSON字符串
     */
    public String getQueryRequestBody(int stan) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("0", "0100");  // 消息类型标识符（0100 表示查询请求）
        requestBody.put("1", "a23840800ac080020000010001000000");  // 位图
        requestBody.put("3", "300000");  // 处理代码
        requestBody.put("7", "1231235959");  // 传输日期和时间
        requestBody.put("11", stan);  // 系统跟踪审计号（STAN）
        requestBody.put("41", "10000007");  // 终端标识符
        requestBody.put("42", "110000000000849");  // 商户标识符

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(requestBody);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create request body", e);
        }
    }

    /**
     * 发送查询请求到FOMO Pay API
     *
     * @param stan 系统跟踪审计号
     * @return API响应内容
     * @throws RuntimeException 当请求失败时抛出
     */
    private String sendQueryRequest(int stan) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(API_URL);

            // 设置 Headers
            Map<String, String> headers = getHeaders();
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                httpPost.setHeader(entry.getKey(), entry.getValue());
            }

            // 设置 Body
            String requestBody = getQueryRequestBody(stan);
            httpPost.setEntity(new StringEntity(requestBody));

            // 发送请求
            HttpResponse response = httpClient.execute(httpPost);
            return EntityUtils.toString(response.getEntity());
        } catch (Exception e) {
            throw new RuntimeException("Failed to send query request", e);
        }
    }

    /**
     * 解析查询响应
     *
     * @param responseBody API返回的响应体
     * @throws RuntimeException 当解析失败时抛出
     */
    private void parseQueryResponse(String responseBody) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);

            String responseCode = (String) responseMap.get("39");
            String errorMessage = (String) responseMap.get("113");

            if ("00".equals(responseCode)) {
                System.out.println("交易支付成功！");
            } else {
                System.out.println("交易支付失败，响应代码：" + responseCode);
                if (errorMessage != null) {
                    System.out.println("错误消息：" + hexToString(errorMessage));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse query response", e);
        }
    }

    /**
     * 处理退款请求
     *
     * @param stan   系统跟踪审计号（6位数字）
     * @param amount 退款金额
     * @return 退款处理结果，包含状态码和错误信息（如果有）
     * @throws RuntimeException 当退款处理失败时抛出
     */
    public String processRefund(String stan, long amount, String retrievalRef, String description) {
        try {
            // 1. 从 static 目录加载私钥和公钥
            PrivateKey privateKey = fomoPayUtil.loadPrivateKey("private_key.pem");
            PublicKey publicKey = fomoPayUtil.loadPublicKey("public_key.pem");

            // 2. 构建退款请求
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode requestBody = objectMapper.createObjectNode();
            String bitmap = calculateBitmap(new int[]{3, 7, 11, 12, 13, 37, 41, 42, 89, 104});

            requestBody.put("0", "0400"); // Message type identifier
            requestBody.put("1", bitmap); // 计算图
            requestBody.put("3", "000000"); // 退款处理码
            requestBody.put("7", "1231235959"); // 发送日期和时间
            // 验证stan是否为6位数字
            if (stan == null || !stan.matches("\\d{6}")) {
                throw new IllegalArgumentException("STAN must be a 6-digit number");
            }
            requestBody.put("11", stan); // 系统跟踪审计号
            requestBody.put("12", "235959"); // 本地交易时间
            requestBody.put("13", "1231"); // 本地交易日期
            requestBody.put("37", retrievalRef); // 交易的检索参考码
            requestBody.put("41", TID); // 终端ID
            requestBody.put("42", MID); // 商户ID
            // 将金额转换为分（cents）并格式化为12位数字
            String formattedAmount = String.format("%012d", amount);
            // 验证格式是否正确
            if (formattedAmount.length() != 12) {
                throw new IllegalArgumentException("Invalid amount format");
            }
            requestBody.put("89", formattedAmount); // 退款金额
            requestBody.put("104", description); // 交易描述

            String payload = objectMapper.writeValueAsString(requestBody);
            System.out.println("payload===" + payload);
            // 3. 生成时间戳和随机数
            long timestamp = System.currentTimeMillis() / 1000;
            String formattedTimestamp = new java.text.SimpleDateFormat("yyyyMMddHHmmss")
                    .format(new java.util.Date(timestamp * 1000));
            String nonce = formattedTimestamp;

            String signature = fomoPayUtil.signRequest(payload, timestamp, nonce, privateKey);

            // 4. 发送退款请求
            Map<String, String> headers = new HashMap<>();
            headers.put("X-Authentication-Version", "1.1");
            headers.put("X-Authentication-Method", "SHA256WithRSA");
            headers.put("X-Authentication-KeyId", KEY_ID);
            headers.put("X-Authentication-Nonce", nonce);
            headers.put("X-Authentication-Timestamp", String.valueOf(timestamp));
            headers.put("X-Authentication-Sign", signature);
            headers.put("Content-Type", "application/json");

            String response = fomoPayUtil.sendHttpPostRequest("https://pos.fomopay.net/rpc", payload, headers);

            // 5. 处理响应
            if (response == null || response.isEmpty()) {
                throw new RuntimeException("Empty response from FOMO Pay API");
            }

            ObjectNode jsonResponse = (ObjectNode) objectMapper.readTree(response);
            String responseCode = jsonResponse.get("39").asText();
            String errorMessage = jsonResponse.has("113") ? hexToString(jsonResponse.get("113").asText()) : null;

            StringBuilder result = new StringBuilder();
            result.append("Refund Result:\n");
            result.append("Status: ").append(responseCode).append("\n");
            if (errorMessage != null) {
                result.append("Error Message: ").append(errorMessage).append("\n");
            }
            result.append(jsonResponse.toPrettyString());

            return result.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }
    /**
     * 待处理支付集合
     */
    private final Set<String> pendingPayments = new HashSet<>();

    /**
     * 添加待处理支付
     *
     * @param transactionId 交易ID
     */
    public void addPendingPayment(String transactionId) {
        pendingPayments.add(transactionId);
    }

    /**
     * 获取所有待处理支付
     *
     * @return 待处理支付列表的字符串表示
     */
    public String getPendingPayments() {
        return "Pending payments:\n" + pendingPayments.toString();
    }

    /**
     * 测试FOMO Pay集成
     * <p>
     * 该方法执行以下操作：
     * 1. 加载加密密钥
     * 2. 构建测试请求
     * 3. 生成签名
     * 4. 发送请求并验证响应
     *
     * @return 测试结果，包含API响应和验证状态
     * @throws RuntimeException 当测试失败时抛出
     */
    public String payIntegration() {
        try {
            // 1. 从 static 目录加载私钥和公钥
            PrivateKey privateKey = fomoPayUtil.loadPrivateKey("private_key.pem");
            PublicKey publicKey = fomoPayUtil.loadPublicKey("public_key.pem");

            // 2. 使用 Jackson 构造 JSON 请求体
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode requestBody = objectMapper.createObjectNode();
            // 根据发送的字段计算位图——位图计算遵循FOMO PayAPI规范
            /**
             * 杂项映射计算现在使用以下字段:
             3(处理代码)
             7(传输日期和时间)
             11(系统跟踪审计号码)
             12(本地交易时间)
             13(本地交易日期)
             18(商户类型)
             25(服务点条件代码)
             41(终端ID)
             42(商户ID)
             49(货币代码)
             88(借记总金额)
             104(交易描述)
             */
            String bitmap = calculateBitmap(new int[]{3, 7, 11, 12, 13, 18, 25, 41, 42, 49, 88, 104});

            requestBody.put("0", "0200"); // Message type identifier
            requestBody.put("1", bitmap); // 计算图
            requestBody.put("3", "000000"); // 交易处理码
            requestBody.put("7", "1231235959"); // 发送日期和时间
            // Generate unique STAN number
            long stanNumber = System.currentTimeMillis() % 1000000;
            requestBody.put("11", String.format("%06d", stanNumber)); // 系统跟踪审计编号
            requestBody.put("12", "235959"); // 本地交易时间
            requestBody.put("13", "1231"); // 本地交易日期
            requestBody.put("18", "0005"); // 商户类型
            requestBody.put("25", "30"); // 服务条件代码
            requestBody.put("41", TID); // 终端ID
            requestBody.put("42", MID); // 商户ID
            requestBody.put("49", "SGD"); // 货币代码
            requestBody.put("88", "000000000001"); // 借记总金额
            requestBody.put("104", "order number:A0000"); // 交易描述

            String payload = objectMapper.writeValueAsString(requestBody);
            System.out.println("payload===" + payload);

            // 3. 生成时间戳和随机数
            // Generate timestamp in required format (YYYYMMDDHHmmss)
            long timestamp = System.currentTimeMillis() / 1000;
            String formattedTimestamp = new java.text.SimpleDateFormat("yyyyMMddHHmmss")
                    .format(new java.util.Date(timestamp * 1000));
            String nonce = formattedTimestamp; // Use formatted timestamp as nonce

            String dataToSign = payload + timestamp + nonce;
            System.out.println("Data to Sign: " + dataToSign);
            // 4. 对请求进行签名
            String signature = fomoPayUtil.signRequest(payload, timestamp, nonce, privateKey);
            System.out.println("Generated Signature: " + signature);

            // 5. 发送 HTTP POST 请求
            Map<String, String> headers = new HashMap<>();
            headers.put("X-Authentication-Version", "1.1");
            headers.put("X-Authentication-Method", "SHA256WithRSA");
            headers.put("X-Authentication-KeyId", KEY_ID); // 使用支付厂家提供的 Key ID
            headers.put("X-Authentication-Nonce", nonce); // 随机数
            headers.put("X-Authentication-Timestamp", String.valueOf(timestamp)); // 时间戳
            headers.put("X-Authentication-Sign", signature); // 签名
            headers.put("Content-Type", "application/json");

            System.out.println("Request Headers: " + headers);

            // 5. 发送 HTTP POST 请求并获取完整响应
            String response = fomoPayUtil.sendHttpPostRequest(API_URL, payload, headers);
            System.out.println("Raw API Response: " + response);

            // 6. 处理响应
            if (response == null || response.isEmpty()) {
                throw new RuntimeException("Empty response from FOMO Pay API");
            }

            try {
                ObjectNode jsonResponse = (ObjectNode) objectMapper.readTree(response);

                // 检查是否是错误响应
                if (jsonResponse.has("error")) {
                    String errorMessage = jsonResponse.get("error").asText();
                    throw new RuntimeException("API Error: " + errorMessage);
                }

                // 获取响应签名 - 可能在headers中
                String responseSignature = null;
                if (jsonResponse.has("X-Authentication-Sign")) {
                    responseSignature = jsonResponse.get("X-Authentication-Sign").asText();
                } else if (jsonResponse.has("headers")) {
                    ObjectNode headersNode = (ObjectNode) jsonResponse.get("headers");
                    if (headersNode.has("X-Authentication-Sign")) {
                        responseSignature = headersNode.get("X-Authentication-Sign").asText();
                    }
                }

                if (responseSignature == null || responseSignature.isEmpty()) {
                    // Decode hex-encoded error messages
                    if (jsonResponse.has("113")) {
                        String hexMessage = jsonResponse.get("113").asText();
                        String decodedMessage = hexToString(hexMessage);
                        return "API Response:\n" +
                                "Status: " + jsonResponse.get("39").asText() + "\n" +
                                "Message: " + decodedMessage + "\n" +
                                "Raw Response: " + jsonResponse.toPrettyString();
                    }
                    return "API Response (no signature verification):\n" + jsonResponse.toPrettyString();
                }

                String responsePayload = jsonResponse.toString();
                boolean isVerified = fomoPayUtil.verifyResponse(responsePayload, responseSignature, publicKey);
                return "API Response:\n" +
                        "Status: " + jsonResponse.get("39").asText() + "\n" +
                        "Signature Verified: " + isVerified + "\n" +
                        "Raw Response: " + jsonResponse.toPrettyString();
            } catch (Exception e) {
                // 如果无法解析为JSON，返回原始响应
                return "API Response (raw): " + response;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }
}
