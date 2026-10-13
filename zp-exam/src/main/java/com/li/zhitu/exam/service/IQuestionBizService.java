package com.li.zhitu.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.li.zhitu.api.dto.exam.QuestionBizDTO;
import com.li.zhitu.exam.domain.po.QuestionBiz;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * 问题和业务关联表，例如把小节id和问题id关联，一个小节下可以有多个问题 服务类
 * </p>
 *
 * @author 李梦豪
 * @since 2022-09-02
 */
public interface IQuestionBizService extends IService<QuestionBiz> {

    int countUsedTimes(Long questionId);

    Map<Long, Integer> countUsedTimes(Set<Long> qIds);

    List<QuestionBizDTO> queryQuestionIdsByBizId(Long bizId);

    List<QuestionBizDTO> queryQuestionIdsByBizIds(List<Long> bizIds);

    void saveQuestionBizInfoBatch(List<QuestionBizDTO> qbs);

    Map<Long, Integer> queryQuestionScoresByBizIds(Iterable<Long> bizIds);
}
