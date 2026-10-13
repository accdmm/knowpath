package com.li.zhitu.trade.service;

import com.li.zhitu.trade.domain.dto.OrderDelayQueryDTO;
import com.li.zhitu.trade.domain.dto.PayApplyFormDTO;
import com.li.zhitu.trade.domain.vo.PayChannelVO;

import java.util.List;

public interface IPayService {
    List<PayChannelVO> queryPayChannels();

    String applyPayOrder(PayApplyFormDTO payApply);

    void queryPayResult(OrderDelayQueryDTO message);
}
