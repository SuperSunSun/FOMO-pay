# FOMO-pay
fomo-pay接口对接

基于交易流程的支付接口使用说明：

创建支付订单 (Sale Request)
使用POST /fomopay/payment接口
示例：
POST /fomopay/payment?amount=100.0&currency=CNY
这将创建一个100元人民币的支付订单

查询支付状态 (Query Request)

使用GET /fomopay/payment-status/{stan}接口
示例：
GET /fomopay/payment-status/123456
这将查询系统跟踪号为123456的交易状态

处理退款请求 (Reversal Request)
使用POST /fomopay/refund接口
示例：
POST /fomopay/refund?stan=123456&amount=50.0&retrievalRef=REF123&description=部分退款
这将处理系统跟踪号为123456的50元退款

提交批次结算 (Batch Submit)
使用GET /fomopay/pending-payments接口
示例：
GET /fomopay/pending-payments
这将获取所有待处理的支付信息用于批次结算