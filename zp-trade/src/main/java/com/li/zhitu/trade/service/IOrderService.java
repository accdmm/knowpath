package com.li.zhitu.trade.service;

import com.li.zhitu.common.domain.dto.PageDTO;
import com.li.zhitu.pay.sdk.dto.PayResultDTO;
import com.li.zhitu.trade.constants.OrderCancelReason;
import com.li.zhitu.trade.domain.dto.PlaceOrderDTO;
import com.li.zhitu.trade.domain.po.Order;
import com.baomidou.mybatisplus.extension.service.IService;
import com.li.zhitu.trade.domain.po.OrderDetail;
import com.li.zhitu.trade.domain.query.OrderPageQuery;
import com.li.zhitu.trade.domain.vo.OrderConfirmVO;
import com.li.zhitu.trade.domain.vo.OrderPageVO;
import com.li.zhitu.trade.domain.vo.OrderVO;
import com.li.zhitu.trade.domain.vo.PlaceOrderResultVO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>
 * 订单 服务类
 * </p>
 *
 * @author 李梦豪
 * @since 2022-08-29
 */
public interface IOrderService extends IService<Order> {

    PlaceOrderResultVO placeOrder(PlaceOrderDTO placeOrderDTO);

    @Transactional
    void saveOrderAndDetails(Order order, List<OrderDetail> orderDetails);

    void cancelOrder(Long orderId, OrderCancelReason cancelReason);

    void deleteOrder(Long id);

    PageDTO<OrderPageVO> queryMyOrderPage(OrderPageQuery pageQuery);

    OrderVO queryOrderById(Long id);

    PlaceOrderResultVO queryOrderStatus(Long orderId);

    void handlePaySuccess(PayResultDTO payResult);

    PlaceOrderResultVO enrolledFreeCourse(Long courseId);

    OrderConfirmVO prePlaceOrder(List<Long> courseIds);

}
