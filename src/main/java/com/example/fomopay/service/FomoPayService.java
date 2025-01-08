package com.example.fomopay.service;

import com.example.fomopay.util.FomoPayUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

@Service
public class FomoPayService {

    @Autowired
    private FomoPayUtil fomoPayUtil;

    // 支付厂家提供的测试参数
    private static final String MID = "110000000000849"; // 商户ID
    private static final String TID = "10000007"; // 终端ID
    private static final String KEY_ID = "a5142d28-7a40-4f39-b22a-1c26287d8aff"; // 密钥ID

    /**
     * 测试 FOMO Pay 支付接口
     *
     * @return 测试结果
     */
    // Helper method to calculate bitmap
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

    // Helper method to convert hex string to readable text
    private String hexToString(String hex) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hex.length(); i += 2) {
            String str = hex.substring(i, i + 2);
            sb.append((char) Integer.parseInt(str, 16));
        }
        return sb.toString();
    }

    public String testFomoPayIntegration() {
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
            String bitmap = calculateBitmap(new int[]{3,7,11,12,13,18,25,41,42,49,88,104});

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
            requestBody.put("104", "Payment Description"); // 交易描述

            String payload = objectMapper.writeValueAsString(requestBody);
            System.out.println("payload==="+payload);

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
            String response = fomoPayUtil.sendHttpPostRequest("https://pos.fomopay.net/rpc", payload, headers);
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
