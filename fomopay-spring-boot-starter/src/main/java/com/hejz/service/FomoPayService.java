package com.hejz.service;

/**
 * FOMO Pay 支付服务实现类
 * <p>
 * 该类负责处理与FOMO Pay支付网关的所有交互，包括：
 * 1. 支付状态查询
 * 2. 退款处理
 * 3. 支付订单创建
 * 4. 支付集成测试
 */
public interface FomoPayService {



    /**
     * 销售
     * <p>
     * 该方法执行以下操作：
     * 1. 加载加密密钥
     * 2. 构建测试请求
     * 3. 生成签名
     * 4. 发送请求并验证响应
     *
     * @param stan
     * @param amount
     * @param description
     * @return 测试结果，包含API响应和验证状态
     * @throws RuntimeException 当测试失败时抛出
     */
    String sale(int stan, long amount, String description) ;


    /**
     * 查询支付状态
     *
     * @param stan 系统跟踪审计号（6位数字）
     * @return 查询结果，包含状态码和交易详情
     * @throws IllegalArgumentException 如果stan不是6位数字
     */
    String query(int stan);

    /**
     * 处理退款请求
     *
     * @param stan   系统跟踪审计号（6位数字）
     * @param amount 退款金额
     * @return 退款处理结果，包含状态码和错误信息（如果有）
     * @throws RuntimeException 当退款处理失败时抛出
     */
    String refund(int stan, long amount, String retrievalRef, String description) ;


    /**
     * 结算
     *
     * @return
     */
     String batchSubmit();

    /**
     * 处理交易作废请求
     *
     * @param stan 系统跟踪号
     * @return 作废处理结果
     */
    String voidTransaction(int stan);


}
