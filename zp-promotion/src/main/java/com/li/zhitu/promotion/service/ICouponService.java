package com.li.zhitu.promotion.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.li.zhitu.common.domain.dto.PageDTO;
import com.li.zhitu.promotion.domain.dto.CouponFormDTO;
import com.li.zhitu.promotion.domain.dto.CouponIssueFormDTO;
import com.li.zhitu.promotion.domain.po.Coupon;
import com.li.zhitu.promotion.domain.query.CouponQuery;
import com.li.zhitu.promotion.domain.vo.CouponDetailVO;
import com.li.zhitu.promotion.domain.vo.CouponPageVO;
import com.li.zhitu.promotion.domain.vo.CouponVO;

import java.util.List;

/**
 * <p>
 * 优惠券的规则信息 服务类
 * </p>
 *
 * @author 李梦豪
 */
public interface ICouponService extends IService<Coupon> {

    void saveCoupon(CouponFormDTO dto);

    PageDTO<CouponPageVO> queryCouponByPage(CouponQuery query);

    void beginIssue(CouponIssueFormDTO dto);

    List<CouponVO> queryIssuingCoupons();

    void pauseIssue(Long id);

    void deleteById(Long id);

    CouponDetailVO queryCouponById(Long id);

    void beginIssueBatch(List<Coupon> coupons);
}
