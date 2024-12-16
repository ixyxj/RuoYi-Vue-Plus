package org.dromara.workflow.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.*;
import org.dromara.common.mybatis.helper.DataBaseHelper;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.warm.flow.core.service.DefService;
import org.dromara.warm.flow.orm.entity.FlowDefinition;
import org.dromara.workflow.common.constant.FlowConstant;
import org.dromara.workflow.domain.FlowCategory;
import org.dromara.workflow.domain.bo.FlowCategoryBo;
import org.dromara.workflow.domain.vo.FlowCategoryVo;
import org.dromara.workflow.mapper.FlwCategoryMapper;
import org.dromara.workflow.service.IFlwCategoryService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 流程分类Service业务层处理
 *
 * @author may
 */
@RequiredArgsConstructor
@Service
public class FlwCategoryServiceImpl implements IFlwCategoryService {

    private final FlwCategoryMapper baseMapper;
    private final DefService defService;

    /**
     * 查询流程分类
     *
     * @param categoryId 主键
     * @return 流程分类
     */
    @Override
    public FlowCategoryVo queryById(Long categoryId) {
        FlowCategoryVo category = baseMapper.selectVoById(categoryId);
        if (ObjectUtil.isNull(category)) {
            return null;
        }
        FlowCategoryVo parentCategory = baseMapper.selectVoOne(new LambdaQueryWrapper<FlowCategory>()
            .select(FlowCategory::getCategoryName).eq(FlowCategory::getCategoryId, category.getParentId()));
        category.setParentName(ObjectUtils.notNullGetter(parentCategory, FlowCategoryVo::getCategoryName));
        return category;
    }

    /**
     * 根据流程分类ID查询流程分类名称
     *
     * @param categoryId 流程分类ID
     * @return 流程分类名称
     */
    @Cacheable(cacheNames = FlowConstant.FLOW_CATEGORY_NAME, key = "#categoryId")
    @Override
    public String selectCategoryNameById(String categoryId) {
        FlowCategory category = baseMapper.selectOne(new LambdaQueryWrapper<FlowCategory>()
            .select(FlowCategory::getCategoryName).eq(FlowCategory::getCategoryId, categoryId));
        return ObjectUtils.notNullGetter(category, FlowCategory::getCategoryName);
    }

    /**
     * 查询符合条件的流程分类列表
     *
     * @param bo 查询条件
     * @return 流程分类列表
     */
    @Override
    public List<FlowCategoryVo> queryList(FlowCategoryBo bo) {
        LambdaQueryWrapper<FlowCategory> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    /**
     * 查询流程分类树结构信息
     *
     * @param category 流程分类信息
     * @return 流程分类树信息集合
     */
    @Override
    public List<Tree<Long>> selectCategoryTreeList(FlowCategoryBo category) {
        LambdaQueryWrapper<FlowCategory> lqw = buildQueryWrapper(category);
        List<FlowCategoryVo> categorys = baseMapper.selectVoList(lqw);
        if (CollUtil.isEmpty(categorys)) {
            return CollUtil.newArrayList();
        }
        // 获取当前列表中每一个节点的parentId，然后在列表中查找是否有id与其parentId对应，若无对应，则表明此时节点列表中，该节点在当前列表中属于顶级节点
        List<Tree<Long>> treeList = CollUtil.newArrayList();
        for (FlowCategoryVo d : categorys) {
            Long parentId = d.getParentId();
            FlowCategoryVo categoryVo = StreamUtils.findFirst(categorys, it -> it.getCategoryId().longValue() == parentId);
            if (ObjectUtil.isNull(categoryVo)) {
                List<Tree<Long>> trees = TreeBuildUtils.build(categorys, parentId, (dept, tree) ->
                    tree.setId(dept.getCategoryId())
                        .setParentId(dept.getParentId())
                        .setName(dept.getCategoryName())
                        .setWeight(dept.getOrderNum())
                        .putExtra("disabled", SystemConstants.DISABLE.equals(dept.getStatus())));
                Tree<Long> tree = StreamUtils.findFirst(trees, it -> it.getId().longValue() == d.getCategoryId());
                treeList.add(tree);
            }
        }
        return treeList;
    }

    /**
     * 校验流程分类是否有数据权限
     *
     * @param categoryId 流程分类ID
     */
    @Override
    public void checkCategoryDataScope(Long categoryId) {
        if (ObjectUtil.isNull(categoryId)) {
            return;
        }
        if (LoginHelper.isSuperAdmin()) {
            return;
        }
        if (baseMapper.countCategoryById(categoryId) == 0) {
            throw new ServiceException("没有权限访问流程分类数据！");
        }
    }

    /**
     * 校验流程分类名称是否唯一
     *
     * @param category 流程分类信息
     * @return 结果
     */
    @Override
    public boolean checkCategoryNameUnique(FlowCategoryBo category) {
        boolean exist = baseMapper.exists(new LambdaQueryWrapper<FlowCategory>()
            .eq(FlowCategory::getCategoryName, category.getCategoryName())
            .eq(FlowCategory::getParentId, category.getParentId())
            .ne(ObjectUtil.isNotNull(category.getCategoryId()), FlowCategory::getCategoryId, category.getCategoryId()));
        return !exist;
    }

    /**
     * 根据ID查询所有子流程分类数（正常状态）
     *
     * @param categoryId 流程分类ID
     * @return 子流程分类数
     */
    @Override
    public long selectNormalChildrenCategoryById(Long categoryId) {
        return baseMapper.selectCount(new LambdaQueryWrapper<FlowCategory>()
            .eq(FlowCategory::getStatus, SystemConstants.NORMAL)
            .apply(DataBaseHelper.findInSet(categoryId, "ancestors")));
    }

    /**
     * 查询流程分类是否存在流程定义
     *
     * @param categoryId 流程分类ID
     * @return 结果 true 存在 false 不存在
     */
    @Override
    public boolean checkCategoryExistDefinition(Long categoryId) {
        FlowDefinition definition = new FlowDefinition();
        definition.setCategory(categoryId.toString());
        return defService.exists(definition);
    }

    /**
     * 是否存在流程分类子节点
     *
     * @param categoryId 流程分类ID
     * @return 结果
     */
    @Override
    public boolean hasChildByCategoryId(Long categoryId) {
        return baseMapper.exists(new LambdaQueryWrapper<FlowCategory>()
            .eq(FlowCategory::getParentId, categoryId));
    }

    private LambdaQueryWrapper<FlowCategory> buildQueryWrapper(FlowCategoryBo bo) {
        LambdaQueryWrapper<FlowCategory> lqw = Wrappers.lambdaQuery();
        lqw.eq(FlowCategory::getDelFlag, SystemConstants.NORMAL);
        lqw.eq(ObjectUtil.isNotNull(bo.getCategoryId()), FlowCategory::getCategoryId, bo.getCategoryId());
        lqw.eq(ObjectUtil.isNotNull(bo.getParentId()), FlowCategory::getParentId, bo.getParentId());
        lqw.like(StringUtils.isNotBlank(bo.getCategoryName()), FlowCategory::getCategoryName, bo.getCategoryName());
        lqw.like(StringUtils.isNotBlank(bo.getCategoryCode()), FlowCategory::getCategoryCode, bo.getCategoryCode());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), FlowCategory::getStatus, bo.getStatus());
        lqw.orderByAsc(FlowCategory::getAncestors);
        lqw.orderByAsc(FlowCategory::getParentId);
        lqw.orderByAsc(FlowCategory::getOrderNum);
        lqw.orderByAsc(FlowCategory::getCategoryId);
        return lqw;
    }

    /**
     * 新增流程分类
     *
     * @param bo 流程分类
     * @return 是否新增成功
     */
    @Override
    public int insertByBo(FlowCategoryBo bo) {
        FlowCategory info = baseMapper.selectById(bo.getParentId());
        // 如果父节点不为正常状态,则不允许新增子节点
        if (!SystemConstants.NORMAL.equals(info.getStatus())) {
            throw new ServiceException("流程分类停用，不允许新增");
        }
        FlowCategory category = MapstructUtils.convert(bo, FlowCategory.class);
        category.setAncestors(info.getAncestors() + StringUtils.SEPARATOR + category.getParentId());
        return baseMapper.insert(category);
    }

    /**
     * 修改流程分类
     *
     * @param bo 流程分类
     * @return 是否修改成功
     */
    @CacheEvict(cacheNames = FlowConstant.FLOW_CATEGORY_NAME, key = "#bo.categoryId")
    @Override
    public int updateByBo(FlowCategoryBo bo) {
        FlowCategory category = MapstructUtils.convert(bo, FlowCategory.class);
        FlowCategory oldCategory = baseMapper.selectById(category.getCategoryId());
        if (ObjectUtil.isNull(oldCategory)) {
            throw new ServiceException("流程分类不存在，无法修改");
        }
        if (!oldCategory.getParentId().equals(category.getParentId())) {
            // 如果是新父流程分类 则校验是否具有新父流程分类权限 避免越权
            this.checkCategoryDataScope(category.getParentId());
            FlowCategory newParentCategory = baseMapper.selectById(category.getParentId());
            if (ObjectUtil.isNotNull(newParentCategory)) {
                String newAncestors = newParentCategory.getAncestors() + StringUtils.SEPARATOR + newParentCategory.getCategoryId();
                String oldAncestors = oldCategory.getAncestors();
                category.setAncestors(newAncestors);
                updateCategoryChildren(category.getCategoryId(), newAncestors, oldAncestors);
            }
        } else {
            category.setAncestors(oldCategory.getAncestors());
        }
        int result = baseMapper.updateById(category);
        if (SystemConstants.NORMAL.equals(category.getStatus()) && StringUtils.isNotEmpty(category.getAncestors())
            && !StringUtils.equals(SystemConstants.NORMAL, category.getAncestors())) {
            // 如果该流程分类是启用状态，则启用该流程分类的所有上级流程分类
            updateParentCategoryStatusNormal(category);
        }
        return result;
    }

    /**
     * 修改子元素关系
     *
     * @param categoryId   被修改的流程分类ID
     * @param newAncestors 新的父ID集合
     * @param oldAncestors 旧的父ID集合
     */
    private void updateCategoryChildren(Long categoryId, String newAncestors, String oldAncestors) {
        List<FlowCategory> children = baseMapper.selectList(new LambdaQueryWrapper<FlowCategory>()
            .apply(DataBaseHelper.findInSet(categoryId, "ancestors")));
        List<FlowCategory> list = new ArrayList<>();
        for (FlowCategory child : children) {
            FlowCategory category = new FlowCategory();
            category.setCategoryId(child.getCategoryId());
            category.setAncestors(child.getAncestors().replaceFirst(oldAncestors, newAncestors));
            list.add(category);
        }
        if (CollUtil.isNotEmpty(list)) {
            baseMapper.updateBatchById(list);
        }
    }

    /**
     * 修改该流程分类的父级流程分类状态
     *
     * @param category 当前流程分类
     */
    private void updateParentCategoryStatusNormal(FlowCategory category) {
        String ancestors = category.getAncestors();
        Long[] categoryIds = Convert.toLongArray(ancestors);
        baseMapper.update(null, new LambdaUpdateWrapper<FlowCategory>()
            .set(FlowCategory::getStatus, SystemConstants.NORMAL)
            .in(FlowCategory::getCategoryId, Arrays.asList(categoryIds)));
    }

    /**
     * 删除流程分类信息
     *
     * @param categoryId 主键
     * @return 是否删除成功
     */
    @CacheEvict(cacheNames = FlowConstant.FLOW_CATEGORY_NAME, key = "#categoryId")
    @Override
    public int deleteWithValidById(Long categoryId) {
        return baseMapper.deleteById(categoryId);
    }
}
