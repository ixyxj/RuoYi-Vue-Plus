package org.dromara.workflow.service;

import org.dromara.workflow.domain.bo.FlowCategoryBo;
import org.dromara.workflow.domain.vo.FlowCategoryVo;

import java.util.Collection;
import java.util.List;

/**
 * 流程分类Service接口
 *
 * @author may
 * @date 2023-06-28
 */
public interface IFlwCategoryService {

    /**
     * 查询流程分类
     */
    FlowCategoryVo queryById(Long id);


    /**
     * 查询流程分类列表
     */
    List<FlowCategoryVo> queryList(FlowCategoryBo bo);

    /**
     * 新增流程分类
     */
    Boolean insertByBo(FlowCategoryBo bo);

    /**
     * 修改流程分类
     */
    Boolean updateByBo(FlowCategoryBo bo);

    /**
     * 校验并批量删除流程分类信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
