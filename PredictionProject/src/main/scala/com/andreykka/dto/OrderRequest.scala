package com.andreykka.dto

import java.time.Instant

case class OrderRequest(
  symbol: String,
  side: OrderSide,
  orderType: OrderType,
  timeInForce: Option[TimeInForce] = None,
  quantity: Option[BigDecimal] = None,
  quoteOrderQty: Option[BigDecimal] = None,
  price: Option[BigDecimal] = None,
  newClientOrderId: Option[String] = None,
  strategyId: Option[Long] = None,
  strategyType: Option[Int] = None,
  stopPrice: Option[BigDecimal] = None,
  trailingDelta: Option[Long] = None,
  icebergQty: Option[BigDecimal] = None,
  newOrderRespType: Option[OrderResponseType] = None,
  selfTradePreventionMode: Option[SelfTradePreventionMode] = None,
  recvWindow: Option[Long] = None,
  timestamp: Long,
)

case class OrderDTO(
  symbol: String,
  orderId: Long,
  orderListId: Long,
  clientOrderId: String,
  transactTime: Instant,
  price: BigDecimal,
  origQty: BigDecimal,
  executedQty: BigDecimal,
  origQuoteOrderQty: BigDecimal,
  cummulativeQuoteQty: BigDecimal,
  status: String,
  timeInForce: String,
  `type`: OrderType,
  side: OrderSide,
  workingTime: Instant,
  selfTradePreventionMode: SelfTradePreventionMode,
)

enum OrderSide:
  case BUY, SELL

enum OrderType:
  case LIMIT, MARKET, STOP_LOSS, STOP_LOSS_LIMIT, TAKE_PROFIT, TAKE_PROFIT_LIMIT, LIMIT_MAKER

enum TimeInForce:
  case GTC, IOC, FOK

enum OrderResponseType:
  case ACK, RESULT, FULL

enum SelfTradePreventionMode:
  case NONE, EXPIRE_TAKER, EXPIRE_MAKER, EXPIRE_BOTH
