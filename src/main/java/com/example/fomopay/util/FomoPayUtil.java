package com.example.fomopay.util;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.pkcs.RSAPrivateKey;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component
public class FomoPayUtil {

    // 从 resources/static 目录加载文件
    private InputStream getResourceFile(String filename) throws IOException {
        ClassPathResource resource = new ClassPathResource("static/" + filename);
        return resource.getInputStream();
    }

    // 从文件加载私钥
    public PrivateKey loadPrivateKey(String filename) throws Exception {
        try (InputStream inputStream = getResourceFile(filename);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            PEMParser pemParser = new PEMParser(reader);
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            Object object = pemParser.readObject();

            if (object instanceof RSAPrivateKey) {
                // 处理传统的 RSA 格式私钥（PKCS#1）
                return converter.getPrivateKey(PrivateKeyInfo.getInstance((RSAPrivateKey) object));
            } else if (object instanceof org.bouncycastle.openssl.PEMKeyPair) {
                // 处理 PEM 格式的密钥对
                return converter.getPrivateKey(((org.bouncycastle.openssl.PEMKeyPair) object).getPrivateKeyInfo());
            } else {
                throw new IllegalArgumentException("Unsupported private key format");
            }
        }
    }

    // 从文件加载公钥
    public PublicKey loadPublicKey(String filename) throws Exception {
        try (InputStream inputStream = getResourceFile(filename);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            PEMParser pemParser = new PEMParser(reader);
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            Object object = pemParser.readObject();

            if (object instanceof org.bouncycastle.openssl.PEMKeyPair) {
                // 处理 PEM 格式的密钥对
                return converter.getPublicKey(((org.bouncycastle.openssl.PEMKeyPair) object).getPublicKeyInfo());
            } else if (object instanceof org.bouncycastle.asn1.x509.SubjectPublicKeyInfo) {
                // 处理公钥
                return converter.getPublicKey((org.bouncycastle.asn1.x509.SubjectPublicKeyInfo) object);
            } else {
                throw new IllegalArgumentException("Unsupported public key format");
            }
        }
    }

    // 对请求进行签名
    public String signRequest(String payload, PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(payload.getBytes(StandardCharsets.UTF_8));
        byte[] signedBytes = signature.sign();
        return bytesToHex(signedBytes).toLowerCase();
    }

    // 验证响应签名
    public boolean verifyResponse(String payload, String signature, PublicKey publicKey) throws Exception {
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(publicKey);
        sig.update(payload.getBytes(StandardCharsets.UTF_8));
        return sig.verify(hexToBytes(signature));
    }

    // 字节数组转十六进制字符串
    public String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // 十六进制字符串转字节数组
    public byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    // 发送 HTTP POST 请求
    public String sendHttpPostRequest(String url, String payload, Map<String, String> headers) throws IOException {
        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);

        // 设置请求头
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            connection.setRequestProperty(entry.getKey(), entry.getValue());
        }

        // 发送请求体
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = payload.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        // 获取响应状态码
        int responseCode = connection.getResponseCode();
        
        // 读取响应
        try (InputStream inputStream = responseCode < 400 ? 
                connection.getInputStream() : connection.getErrorStream();
             BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            
            if (responseCode >= 400) {
                throw new IOException("HTTP Error " + responseCode + ": " + response.toString());
            }
            return response.toString();
        } finally {
            connection.disconnect();
        }
    }
    public String signRequest(String payload, long timestamp, String nonce, PrivateKey privateKey) throws Exception {
        // 拼接 payload、timestamp 和 nonce
        String dataToSign = payload + timestamp + nonce;

        // 使用 SHA256WithRSA 进行签名
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(dataToSign.getBytes(StandardCharsets.UTF_8));
        byte[] signedBytes = signature.sign();

        // 将签名转换为小写十六进制字符串
        return bytesToHex(signedBytes).toLowerCase();
    }

}
