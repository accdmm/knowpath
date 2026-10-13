package com.li.zhitu.promotion.strategy.scope;

import com.li.zhitu.api.dto.promotion.OrderCourseDTO;
import com.li.zhitu.promotion.constants.ScopeType;

import java.util.List;

public interface Scope {

    boolean canUse(OrderCourseDTO course);

    ScopeType getType();

    List<Long> getScopeIds();
}
