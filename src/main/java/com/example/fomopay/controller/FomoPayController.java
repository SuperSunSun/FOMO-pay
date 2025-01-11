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
     * 处理销售交易
     * 接口路径: GET /fomopay/transactions/sale
     * 
     * @param stan        系统跟踪号 (System Trace Audit Number)
     * @param amount      交易金额 (Transaction amount)
     * @param description 交易描述 (Transaction description)
     * @return 交易处理结果 (Transaction processing result)
     * @throws Exception 处理过程中可能发生的异常 (Possible exceptions during processing)
     */
    @GetMapping("/transactions/sale")
    public String processSale(@RequestParam int stan,
                              @RequestParam long amount,
                              @RequestParam String description) {
        try {
            return fomoPayService.sale(stan, amount, description);
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

    /**
     * 查询交易状态
     * 接口路径: GET /fomopay/transactions/{stan}
     * 
     * @param stan 系统跟踪号 (System Trace Audit Number)
     * @return 交易状态信息 (Transaction status information)
     * @throws Exception 查询过程中可能发生的异常 (Possible exceptions during query)
     */
    @GetMapping("/transactions/{stan}")
    public String getTransactionStatus(@PathVariable int stan) {
        try {
            return fomoPayService.query(stan);
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

    /**
     * 处理退款请求
     * 接口路径: PUT /fomopay/transactions
     * 
     * @param stan        系统跟踪号 (System Trace Audit Number)
     * @param amount      退款金额 (Refund amount)
     * @param retrievalRef 检索参考号 (Retrieval Reference Number)
     * @param description 退款描述 (Refund description)
     * @return 退款处理结果 (Refund processing result)
     * @throws Exception 处理过程中可能发生的异常 (Possible exceptions during processing)
     */
    @PutMapping("/transactions")
    public String processRefund(@RequestParam int stan,
                                @RequestParam int amount,
                                @RequestParam String retrievalRef,
                                @RequestParam String description
    ) {
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
     * 执行批量结算
     * 接口路径: GET /fomopay/transactions/batch-settlement
     * 
     * @return 结算结果 (Settlement result)
     * @throws Exception 结算过程中可能发生的异常 (Possible exceptions during settlement)
     */
    @GetMapping("/transactions/batch-settlement")
    public String processBatchSettlement() {
        try {
            return fomoPayService.batchSubmit();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

    /**
     * 处理交易作废请求
     * 接口路径: DELETE /fomopay/transactions/{stan}
     * 
     * @param stan 系统跟踪号 (System Trace Audit Number)
     * @return 作废处理结果 (Void transaction result)
     * @throws Exception 处理过程中可能发生的异常 (Possible exceptions during processing)
     */
    @DeleteMapping("/transactions/{stan}")
    public String processVoidTransaction(@PathVariable int stan) {
        try {
            return fomoPayService.voidTransaction(
                    stan
            );
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }
}
