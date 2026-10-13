package com.li.zhitu.promotion.service.impl;

import com.li.zhitu.promotion.domain.po.Promotion;
import com.li.zhitu.promotion.mapper.PromotionMapper;
import com.li.zhitu.promotion.service.IPromotionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 促销活动，形式多种多样，例如：优惠券 服务实现类
 * </p>
 *
 * @author 李梦豪
 * @since 2022-09-06
 */
@Service
public class PromotionServiceImpl extends ServiceImpl<PromotionMapper, Promotion> implements IPromotionService {

}
