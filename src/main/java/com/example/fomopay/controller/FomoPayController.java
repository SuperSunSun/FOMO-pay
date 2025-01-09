package com.example.fomopay.controller;

import com.example.fomopay.service.FomoPayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/fomopay")
public class FomoPayController {

    @Autowired
    private FomoPayService fomoPayService;

    /**
     * 测试FomoPay集成是否正常
     * @return 测试结果
     */
    @GetMapping("/pay")
    public String testFomoPayIntegration() {
        try {
            return fomoPayService.payIntegration();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

    /**
     * 查询支付状态
     * @param stan 系统跟踪号
     * @return 支付状态信息
     */
    @GetMapping("/payment-status/{stan}")
    public String getPaymentStatus(@PathVariable("stan") int stan) {
        try {
            return fomoPayService.getQueryRequestBody(stan);
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

    /**
     * 处理退款请求
     * @param stan 系统跟踪号
     * @param amount 退款金额
     * @return 退款处理结果
     */
    /**
     * 处理退款请求
     * @param stan 系统跟踪号（6位数字）
     * @param amount 退款金额
     * @param retrievalRef 检索参考号（来自原始交易）
     * @param description 交易描述
     * @return 退款处理结果
     */
    @PostMapping("/refund")
    public String processRefund(@RequestParam(required = true) String stan,
                              @RequestParam(required = true) double amount,
                              @RequestParam(required = true) String retrievalRef,
                              @RequestParam(required = true) String description) {
        try {
            // 将金额转换为分（API要求最小货币单位）
            long amountInCents = (long)amount;
            
            // 调用服务层处理退款
            return fomoPayService.processRefund(
                stan,
                amountInCents,
                retrievalRef,
                description
            );
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

    /**
     * 创建支付订单
     * @param transactionId 交易ID
     * @return 支付创建结果
     */
    @PostMapping("/payment")
    public String createPayment(@RequestParam(required = false, defaultValue = "") String transactionId) {
        try {
            fomoPayService.addPendingPayment(transactionId);
            return "Payment created successfully. Transaction ID: " + transactionId;
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

    /**
     * 获取待处理支付列表
     * @return 待处理支付信息
     */
    @GetMapping("/pending-payments")
    public String getPendingPayments() {
        try {
            return fomoPayService.getPendingPayments();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }
}
