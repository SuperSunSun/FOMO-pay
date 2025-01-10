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
     * 销售
     *
     * @return 销售数据
     */
    @GetMapping("/sale")
    public String sale(@RequestParam(required = true) int stan,
                       @RequestParam(required = true) int amount,
                       @RequestParam(required = true) String description) {
        try {
            return fomoPayService.sale(stan, amount,description);
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

    /**
     * 查询支付状态
     *
     * @param stan 系统跟踪号
     * @return 支付状态信息
     */
    @GetMapping("/query/{stan}")
    public String getPaymentStatus(@PathVariable("stan") int stan) {
        try {
            return fomoPayService.query(stan);
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

    /**
     * 处理退款请求
     *
     * @param stan         系统跟踪号（6位数字）
     * @param amount       退款金额
     * @param retrievalRef 检索参考号（来自原始交易）
     * @param description  交易描述
     * @return 退款处理结果
     */
    @PostMapping("/refund")
    public String refund(@RequestParam(required = true) String stan,
                           @RequestParam(required = true) int amount,
                           @RequestParam(required = true) String retrievalRef,
                           @RequestParam(required = true) String description) {
        try {

            // 调用服务层处理退款
            return fomoPayService.refund(
                    stan,
                    amount,
                    retrievalRef,
                    description
            );
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }


    /**
     * 结算
     *
     * @return 结算
     */
    @GetMapping("/batchSubmit")
    public String batchSubmit() {
        try {
            return fomoPayService.batchSubmit();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }
}
