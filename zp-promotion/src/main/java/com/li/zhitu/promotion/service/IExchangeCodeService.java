package com.li.zhitu.promotion.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.li.zhitu.common.domain.dto.PageDTO;
import com.li.zhitu.promotion.domain.po.Coupon;
import com.li.zhitu.promotion.domain.po.ExchangeCode;
import com.li.zhitu.promotion.domain.query.CodeQuery;
import com.li.zhitu.promotion.domain.vo.ExchangeCodeVO;

/**
 * <p>
 * 兑换码 服务类
 * </p>
 *
 * @author 李梦豪
 */
public interface IExchangeCodeService extends IService<ExchangeCode> {
    void asyncGenerateCode(Coupon coupon);

    boolean updateExchangeMark(long serialNum, boolean mark);

    PageDTO<ExchangeCodeVO> queryCodePage(CodeQuery query);

    Long exchangeTargetId(long serialNum);
}
