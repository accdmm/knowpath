package com.li.zhitu.search.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.li.zhitu.search.domain.po.Interests;
import com.li.zhitu.api.dto.course.CategoryBasicDTO;

import java.util.List;

/**
 * <p>
 * 用户兴趣表，保存感兴趣的二级分类id 服务类
 * </p>
 *
 * @author 李梦豪
 * @since 2022-07-21
 */
public interface IInterestsService extends IService<Interests> {

    void saveInterests(List<Long> interestedIds);

    List<CategoryBasicDTO> queryMyInterests();

    List<Long> queryMyInterestsIds();
}
