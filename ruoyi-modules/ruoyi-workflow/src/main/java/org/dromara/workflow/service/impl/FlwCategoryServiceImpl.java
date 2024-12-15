package org.dromara.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.workflow.domain.FlowCategory;
import org.dromara.workflow.domain.bo.FlowCategoryBo;
import org.dromara.workflow.domain.vo.FlowCategoryVo;
import org.dromara.workflow.mapper.FlwCategoryMapper;
import org.dromara.workflow.service.IFlwCategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * 流程分类Service业务层处理
 *
 * @author may
 * @date 2023-06-28
 */
@RequiredArgsConstructor
@Service
public class FlwCategoryServiceImpl implements IFlwCategoryService {

    private final FlwCategoryMapper baseMapper;

    /**
     * 查询流程分类
     */
    @Override
    public FlowCategoryVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }


    /**
     * 查询流程分类列表
     */
    @Override
    public List<FlowCategoryVo> queryList(FlowCategoryBo bo) {
        LambdaQueryWrapper<FlowCategory> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<FlowCategory> buildQueryWrapper(FlowCategoryBo bo) {
        LambdaQueryWrapper<FlowCategory> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getCategoryName()), FlowCategory::getCategoryName, bo.getCategoryName());
        return lqw;
    }

    /**
     * 新增流程分类
     */
    @Override
    public Boolean insertByBo(FlowCategoryBo bo) {
        FlowCategory add = MapstructUtils.convert(bo, FlowCategory.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改流程分类
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(FlowCategoryBo bo) {
        FlowCategory update = MapstructUtils.convert(bo, FlowCategory.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(FlowCategory entity) {
        // 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除流程分类
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteByIds(ids) > 0;
    }
}
