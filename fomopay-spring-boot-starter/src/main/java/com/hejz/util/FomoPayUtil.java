package com.hejz.util;

/**
 * FomoPay工具类
 * 
 * <p>提供与FomoPay支付网关交互所需的各种工具方法，包括：</p>
 * <ul>
 *   <li>RSA密钥加载（支持PKCS#1和PKCS#8格式）</li>
 *   <li>请求签名生成与验证</li>
 *   <li>HTTP请求发送</li>
 *   <li>数据格式转换</li>
 * </ul>
 */

import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Map;

@Component
public class FomoPayUtil {

    /**
     * 从resources/static目录加载文件
     * 
     * <p>该方法用于从classpath下的static目录加载文件资源，主要用于加载PEM格式的密钥文件。</p>
     * 
     * @param filename 要加载的文件名，例如："private_key.pem"
     * @return 文件输入流，可用于读取文件内容
     * @throws IOException 如果文件不存在或读取失败
     * @see ClassPathResource
     */
    private InputStream getResourceFile(String filename) throws IOException {
        // 使用Spring的ClassPathResource加载资源文件
        ClassPathResource resource = new ClassPathResource("static/" + filename);
        // 返回输入流以便后续读取
        return resource.getInputStream();
    }

    /**
     * 从PEM文件加载私钥
     * 
     * <p>该方法用于从PEM格式的文件中加载RSA私钥，支持以下格式：</p>
     * <ul>
     *   <li>PKCS#1格式的传统RSA私钥</li>
     *   <li>PKCS#8格式的私钥</li>
     *   <li>PEM格式的密钥对</li>
     * </ul>
     * 
     * <p>使用BouncyCastle库解析PEM文件，并自动识别密钥格式。</p>
     * 
     * @param filename PEM格式的私钥文件名，例如："private_key.pem"
     * @return 加载的PrivateKey对象，可用于签名操作
     * @throws Exception 如果密钥格式不支持或加载失败
     * @throws IllegalArgumentException 如果密钥格式不被支持
     * @see PEMParser
     * @see JcaPEMKeyConverter
     */
    public PrivateKey loadPrivateKey(String filename) throws Exception {
        try (InputStream inputStream = getResourceFile(filename);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
             PEMParser pemParser = new PEMParser(reader)) {
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            Object object = pemParser.readObject();

            if (object instanceof org.bouncycastle.openssl.PEMKeyPair) {
                // 处理 PEM 格式的密钥对
                return converter.getPrivateKey(((org.bouncycastle.openssl.PEMKeyPair) object).getPrivateKeyInfo());
            } else if (object instanceof org.bouncycastle.asn1.pkcs.PrivateKeyInfo) {
                // 处理 PKCS#8 格式的私钥
                return converter.getPrivateKey((org.bouncycastle.asn1.pkcs.PrivateKeyInfo) object);
            } else {
                throw new IllegalArgumentException("Unsupported private key format");
            }
        }
    }

    /**
     * 从PEM文件加载公钥
     * 
     * <p>该方法用于从PEM格式的文件中加载RSA公钥，支持以下格式：</p>
     * <ul>
     *   <li>X.509格式的公钥</li>
     *   <li>PEM格式的密钥对</li>
     * </ul>
     * 
     * <p>使用BouncyCastle库解析PEM文件，并自动识别密钥格式。</p>
     * 
     * <p>注意：公钥文件通常以"-----BEGIN PUBLIC KEY-----"开头。</p>
     * 
     * @param filename PEM格式的公钥文件名，例如："public_key.pem"
     * @return 加载的PublicKey对象，可用于验证签名
     * @throws Exception 如果密钥格式不支持或加载失败
     * @throws IllegalArgumentException 如果密钥格式不被支持
     * @see PEMParser
     * @see JcaPEMKeyConverter
     */
    public PublicKey loadPublicKey(String filename) throws Exception {
        try (InputStream inputStream = getResourceFile(filename);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
             PEMParser pemParser = new PEMParser(reader)) {
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            Object object = pemParser.readObject();

            if (object instanceof org.bouncycastle.openssl.PEMKeyPair) {
                // 处理 PEM 格式的密钥对
                return converter.getPublicKey(((org.bouncycastle.openssl.PEMKeyPair) object).getPublicKeyInfo());
            } else if (object instanceof org.bouncycastle.asn1.x509.SubjectPublicKeyInfo) {
                // 处理 X.509 格式的公钥
                return converter.getPublicKey((org.bouncycastle.asn1.x509.SubjectPublicKeyInfo) object);
            } else {
                throw new IllegalArgumentException("Unsupported public key format");
            }
        }
    }


    /**
     * 验证响应签名
     * 
     * <p>该方法用于验证FomoPay API返回的响应签名，确保响应数据的完整性和真实性。</p>
     * 
     * <p>验证过程：</p>
     * <ol>
     *   <li>使用SHA-256算法对原始数据进行哈希</li>
     *   <li>使用RSA公钥对签名进行解密</li>
     *   <li>比较解密后的哈希值与计算得到的哈希值</li>
     * </ol>
     * 
     * <p>注意：该方法用于验证FomoPay API返回的响应签名，确保响应未被篡改。</p>
     * 
     * @param payload 原始数据，通常为JSON字符串
     * @param signature 要验证的签名，十六进制字符串格式
     * @param publicKey 用于验证的RSA公钥，可通过loadPublicKey方法加载
     * @return 验证结果，true表示签名有效
     * @throws Exception 如果验证过程失败
     * @throws InvalidKeyException 如果公钥无效
     * @throws SignatureException 如果验证过程出错
     * @see #loadPublicKey(String)
     */
    public boolean verifyResponse(String payload, String signature, PublicKey publicKey) throws Exception {
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(publicKey);
        sig.update(payload.getBytes(StandardCharsets.UTF_8));
        return sig.verify(hexToBytes(signature));
    }

    /**
     * 将字节数组转换为十六进制字符串
     * 
     * <p>该方法用于将字节数组转换为小写十六进制字符串表示，主要用于：</p>
     * <ul>
     *   <li>将签名结果转换为可传输的字符串格式</li>
     *   <li>将加密数据转换为可读格式</li>
     *   <li>调试时查看二进制数据的十六进制表示</li>
     * </ul>
     * 
     * <p>转换规则：</p>
     * <ol>
     *   <li>每个字节转换为2个十六进制字符</li>
     *   <li>使用小写字母表示a-f</li>
     *   <li>不足两位的前面补零</li>
     * </ol>
     * 
     * @param bytes 要转换的字节数组，通常为签名或加密结果
     * @return 小写十六进制字符串，长度为字节数组长度的两倍
     */
    public String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 将十六进制字符串转换为字节数组
     * 
     * <p>该方法用于将十六进制字符串转换回字节数组，主要用于：</p>
     * <ul>
     *   <li>将API返回的签名字符串转换为字节数组进行验证</li>
     *   <li>将十六进制格式的加密数据还原为原始字节</li>
     *   <li>处理从外部系统接收的十六进制数据</li>
     * </ul>
     * 
     * <p>转换规则：</p>
     * <ol>
     *   <li>每两个十六进制字符转换为一个字节</li>
     *   <li>支持大小写字母（A-F或a-f）</li>
     *   <li>字符串长度必须为偶数</li>
     * </ol>
     * 
     * @param hex 要转换的十六进制字符串，通常为API返回的签名或加密数据
     * @return 转换后的字节数组，长度为字符串长度的一半
     * @throws IllegalArgumentException 如果字符串长度为奇数或包含非法字符
     */
    public byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * 发送HTTP POST请求
     * 
     * <p>该方法用于向指定URL发送HTTP POST请求，主要用于与FomoPay API进行交互。</p>
     * 
     * <p>请求过程：</p>
     * <ol>
     *   <li>创建HTTP连接并设置请求方法为POST</li>
     *   <li>设置请求头信息</li>
     *   <li>将payload写入请求体</li>
     *   <li>发送请求并获取响应</li>
     * </ol>
     * 
     * <p>错误处理：</p>
     * <ul>
     *   <li>如果响应码>=400，抛出包含错误信息的IOException</li>
     *   <li>自动处理输入输出流的关闭</li>
     *   <li>确保连接最终被断开</li>
     * </ul>
     * 
     * <p>注意：</p>
     * <ul>
     *   <li>请求体使用UTF-8编码</li>
     *   <li>响应内容会被trim()处理</li>
     *   <li>支持自定义请求头</li>
     * </ul>
     * 
     * @param url 请求的目标URL，必须为有效的HTTP/HTTPS地址
     * @param payload 请求体内容，通常为JSON格式的字符串
     * @param headers 请求头Map，key为头名称，value为头值
     * @return 响应内容字符串，已去除前后空白字符
     * @throws IOException 如果请求失败、网络连接问题或响应码>=400
     * @throws IllegalArgumentException 如果URL格式无效
     */
    public String sendHttpPostRequest(String url, String payload, Map<String, String> headers) throws IOException {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }
        
        try {
            // Validate URL format
            java.net.URI uri = new java.net.URI(url);
            // 使用uri.toURL()替代new URL(String)
            javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(
                (hostname, session) -> hostname.equals("pos.fomopay.net")
            );
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(5000); // 5 seconds
            connection.setReadTimeout(10000); // 10 seconds

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
                    throw new IOException("HTTP Error " + responseCode + ": " + response);
                }
                return response.toString();
            } finally {
                connection.disconnect();
            }
        } catch (java.net.URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL format: " + url, e);
        }
    }
    /**
     * 带时间戳和随机数的签名方法
     * 
     * <p>该方法用于生成包含时间戳和随机数的请求签名，主要用于防止重放攻击。</p>
     * 
     * <p>签名过程：</p>
     * <ol>
     *   <li>将payload、timestamp和nonce拼接成待签名字符串</li>
     *   <li>使用SHA256withRSA算法对拼接后的字符串进行签名</li>
     *   <li>将签名结果转换为小写十六进制字符串</li>
     * </ol>
     * 
     * <p>安全特性：</p>
     * <ul>
     *   <li>时间戳用于防止过期请求</li>
     *   <li>随机数确保每次请求签名唯一</li>
     *   <li>签名结果不可逆，无法伪造</li>
     * </ul>
     * 
     * <p>注意：</p>
     * <ul>
     *   <li>timestamp应为当前时间的毫秒数</li>
     *   <li>nonce应为唯一的随机字符串</li>
     *   <li>服务器端需要验证时间戳和nonce的唯一性</li>
     * </ul>
     * 
     * @param payload 原始数据，通常为JSON字符串
     * @param timestamp 时间戳，单位为毫秒
     * @param nonce 随机字符串，建议使用UUID
     * @param privateKey 用于签名的RSA私钥，可通过loadPrivateKey方法加载
     * @return 签名结果的十六进制字符串（小写）
     * @throws Exception 如果签名过程失败
     * @throws InvalidKeyException 如果私钥无效
     * @throws SignatureException 如果签名过程出错
     * @see #loadPrivateKey(String)
     */
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

    /**
     * 通用的位图计算方法
     *
     * @param fields 存在的字段编号（例如：{3, 7, 11, 12, 13, 18, 25, 41, 42, 49, 54, 62, 88, 104}）
     * @return 位图（十六进制字符串）
     */
    public String calculateBitmap(int[] fields) {
        // 初始化位图（128 位，支持主位图和次位图）
        boolean[] bitmap = new boolean[128];

        // 设置存在的字段对应的位
        for (int field : fields) {
            if (field > 1 && field <= 128) {
                bitmap[field - 1] = true; // 字段编号从 1 开始，位图索引从 0 开始
            }
        }

        // 判断是否需要次位图
        boolean hasSecondaryBitmap = false;
        for (int i = 64; i < 128; i++) {
            if (bitmap[i]) {
                hasSecondaryBitmap = true;
                break;
            }
        }

        // 如果存在次位图，设置主位图的第 1 位为 1
        if (hasSecondaryBitmap) {
            bitmap[0] = true;
        }

        // 将位图转换为十六进制字符串
        StringBuilder hexBitmap = new StringBuilder();
        for (int i = 0; i < (hasSecondaryBitmap ? 128 : 64); i += 4) {
            int value = 0;
            for (int j = 0; j < 4; j++) {
                if (bitmap[i + j]) {
                    value |= (1 << (3 - j)); // 将 4 位二进制转换为 1 位十六进制
                }
            }
            hexBitmap.append(Integer.toHexString(value));
        }

        return hexBitmap.toString();
    }

}
