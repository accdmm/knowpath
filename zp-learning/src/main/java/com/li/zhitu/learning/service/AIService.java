package com.li.zhitu.learning.service;

import com.li.zhitu.learning.domain.po.InteractionQuestion;

public interface AIService {

    /**
     * AI 自动回复
     *
     * @param interactionQuestion 问题对象
     */
    void autoReply(InteractionQuestion interactionQuestion);

}