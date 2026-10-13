package com.li.zhitu.learning.service;

import com.li.zhitu.common.domain.dto.PageDTO;
import com.li.zhitu.learning.domain.dto.NoteFormDTO;
import com.li.zhitu.learning.domain.po.Note;
import com.baomidou.mybatisplus.extension.service.IService;
import com.li.zhitu.learning.domain.query.NoteAdminPageQuery;
import com.li.zhitu.learning.domain.query.NotePageQuery;
import com.li.zhitu.learning.domain.vo.NoteAdminDetailVO;
import com.li.zhitu.learning.domain.vo.NoteAdminVO;
import com.li.zhitu.learning.domain.vo.NoteVO;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 李梦豪
 */
public interface INoteService extends IService<Note> {

    void saveNote(NoteFormDTO noteDTO);

    void gatherNote(Long id);

    void removeGatherNote(Long id);

    void updateNote(NoteFormDTO noteDTO);

    PageDTO<NoteVO> queryNotePage(NotePageQuery query);

    PageDTO<NoteAdminVO> queryNotePageForAdmin(NoteAdminPageQuery query);

    NoteAdminDetailVO queryNoteDetailForAdmin(Long id);

    void hiddenNote(Long id, boolean hidden);

    void removeMyNote(Long id);
}
