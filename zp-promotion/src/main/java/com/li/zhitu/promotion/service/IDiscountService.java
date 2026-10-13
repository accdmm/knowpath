package com.li.zhitu.promotion.service;

import com.li.zhitu.api.dto.promotion.CouponDiscountDTO;
import com.li.zhitu.api.dto.promotion.OrderCouponDTO;
import com.li.zhitu.api.dto.promotion.OrderCourseDTO;

import java.util.List;

public interface IDiscountService {
    List<CouponDiscountDTO> findDiscountSolution(List<OrderCourseDTO> orderCourses);

    CouponDiscountDTO queryDiscountDetailByOrder(OrderCouponDTO orderCouponDTO);
}
