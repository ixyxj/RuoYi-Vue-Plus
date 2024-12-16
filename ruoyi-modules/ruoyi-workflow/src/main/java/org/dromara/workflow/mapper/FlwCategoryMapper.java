package org.dromara.workflow.mapper;

import org.dromara.common.mybatis.annotation.DataColumn;
import org.dromara.common.mybatis.annotation.DataPermission;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.workflow.domain.FlowCategory;
import org.dromara.workflow.domain.vo.FlowCategoryVo;

/**
 * 流程分类Mapper接口
 *
 * @author may
 * @date 2023-06-27
 */
public interface FlwCategoryMapper extends BaseMapperPlus<FlowCategory, FlowCategoryVo> {

    /**
     * 统计指定流程分类ID的分类数量
     *
     * @param categoryId 流程分类ID
     * @return 该流程分类ID的分类数量
     */
    @DataPermission({
        @DataColumn(key = "deptName", value = "createDept")
    })
    long countCategoryById(Long categoryId);

}
