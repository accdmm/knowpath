package com.li.zhitu.learning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.li.zhitu.common.domain.dto.PageDTO;
import com.li.zhitu.learning.domain.dto.QuestionFormDTO;
import com.li.zhitu.learning.domain.po.InteractionQuestion;
import com.li.zhitu.learning.domain.query.QuestionAdminPageQuery;
import com.li.zhitu.learning.domain.query.QuestionPageQuery;
import com.li.zhitu.learning.domain.vo.QuestionAdminVO;
import com.li.zhitu.learning.domain.vo.QuestionVO;

/**
 * <p>
 * 互动提问的问题表 服务类
 * </p>
 *
 * @author 李梦豪
 */
public interface IInteractionQuestionService extends IService<InteractionQuestion> {

    InteractionQuestion saveQuestion(QuestionFormDTO questionDTO);

    PageDTO<QuestionVO> queryQuestionPage(QuestionPageQuery query);

    QuestionVO queryQuestionById(Long id);

    PageDTO<QuestionAdminVO> queryQuestionPageAdmin(QuestionAdminPageQuery query);

    QuestionAdminVO queryQuestionByIdAdmin(Long id);

    void hiddenQuestion(Long id, Boolean hidden);

    void updateQuestion(Long id, QuestionFormDTO questionDTO);

    void deleteById(Long id);
}
