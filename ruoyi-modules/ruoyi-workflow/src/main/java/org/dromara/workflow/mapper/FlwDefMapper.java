package org.dromara.workflow.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warm.flow.orm.entity.FlowDefinition;
import org.apache.ibatis.annotations.Param;

/**
 * 实例信息Mapper接口
 *
 * @author may
 * @date 2024-03-02
 */
public interface FlwDefMapper {

    /**
     * 流程实例信息
     *
     * @param page         分页
     * @param queryWrapper 条件
     * @return 结果
     */
    Page<FlowDefinition> page(@Param("page") Page<FlowDefinition> page, @Param(Constants.WRAPPER) Wrapper<FlowDefinition> queryWrapper);

}
