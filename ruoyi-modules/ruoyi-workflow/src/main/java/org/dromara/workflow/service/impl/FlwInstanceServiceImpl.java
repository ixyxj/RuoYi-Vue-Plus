package org.dromara.workflow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warm.flow.core.entity.*;
import com.warm.flow.core.enums.CooperateType;
import com.warm.flow.core.enums.FlowStatus;
import com.warm.flow.core.enums.NodeType;
import com.warm.flow.core.service.*;
import com.warm.flow.orm.entity.FlowDefinition;
import com.warm.flow.orm.entity.FlowHisTask;
import com.warm.flow.orm.entity.FlowInstance;
import com.warm.flow.orm.mapper.FlowDefinitionMapper;
import com.warm.flow.orm.mapper.FlowHisTaskMapper;
import com.warm.flow.orm.mapper.FlowInstanceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.constant.UserConstants;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.workflow.domain.bo.FlowInstanceBo;
import org.dromara.workflow.domain.bo.InstanceBo;
import org.dromara.workflow.domain.vo.FlowHisTaskVo;
import org.dromara.workflow.domain.vo.FlowInstanceVo;
import org.dromara.workflow.mapper.FlwInstanceMapper;
import org.dromara.workflow.service.IFlwInstanceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 流程实例 服务层实现
 *
 * @author may
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class FlwInstanceServiceImpl implements IFlwInstanceService {

    private final InsService insService;
    private final DefService defService;
    private final FlowHisTaskMapper flowHisTaskMapper;
    private final FlowInstanceMapper flowInstanceMapper;
    private final FlwInstanceMapper flwInstanceMapper;
    private final FlowDefinitionMapper flowDefinitionMapper;

    /**
     * 分页查询正在运行的流程实例
     *
     * @param instance  参数
     * @param pageQuery 分页
     */
    @Override
    public TableDataInfo<FlowInstanceVo> getPageByRunning(Instance instance, PageQuery pageQuery) {
        QueryWrapper<FlowInstanceBo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("t.flow_status", FlowStatus.APPROVAL.getKey());
        queryWrapper.eq("t.del_flag", UserConstants.USER_NORMAL);
        Page<FlowInstanceVo> page = flwInstanceMapper.page(pageQuery.build(), queryWrapper);
        TableDataInfo<FlowInstanceVo> build = TableDataInfo.build();
        build.setRows(BeanUtil.copyToList(page.getRecords(), FlowInstanceVo.class));
        build.setTotal(page.getTotal());
        return build;
    }

    /**
     * 分页查询已结束的流程实例
     *
     * @param instance  参数
     * @param pageQuery 分页
     */
    @Override
    public TableDataInfo<FlowInstanceVo> getPageByFinish(Instance instance, PageQuery pageQuery) {
        QueryWrapper<FlowInstanceBo> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("t.flow_status", Arrays.asList(FlowStatus.FINISHED.getKey(), FlowStatus.AUTO_PASS.getKey()));
        Page<FlowInstanceVo> page = flwInstanceMapper.page(pageQuery.build(), queryWrapper);
        TableDataInfo<FlowInstanceVo> build = TableDataInfo.build();
        build.setRows(BeanUtil.copyToList(page.getRecords(), FlowInstanceVo.class));
        build.setTotal(page.getTotal());
        return build;
    }

    /**
     * 根据业务id查询流程实例
     *
     * @param businessId 业务id
     */
    @Override
    public FlowInstance instanceByBusinessId(String businessId) {
        return flowInstanceMapper.selectOne(new LambdaQueryWrapper<FlowInstance>().eq(FlowInstance::getBusinessId, businessId));
    }

    /**
     * 按照业务id删除流程实例
     *
     * @param businessIds 业务id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByBusinessIds(List<Long> businessIds) {
        List<FlowInstance> flowInstances = flowInstanceMapper.selectList(new LambdaQueryWrapper<FlowInstance>().in(FlowInstance::getBusinessId, businessIds));
        if (CollUtil.isEmpty(flowInstances)) {
            return false;
        }
        return insService.remove(StreamUtils.toList(flowInstances, FlowInstance::getId));
    }

    /**
     * 按照实例id删除流程实例
     *
     * @param instanceIds 实例id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByInstanceIds(List<Long> instanceIds) {
        return insService.remove(instanceIds);
    }

    /**
     * 撤销流程
     *
     * @param businessId 业务id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelProcessApply(String businessId) {
        throw new RuntimeException("暂未开发");
    }

    /**
     * 获取当前登陆人发起的流程实例
     *
     * @param instanceBo 参数
     * @param pageQuery  分页
     */
    @Override
    public TableDataInfo<FlowInstanceVo> getPageByCurrent(InstanceBo instanceBo, PageQuery pageQuery) {
        LambdaQueryWrapper<FlowInstance> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.isNotBlank(instanceBo.getFlowCode())) {
            List<FlowDefinition> flowDefinitions = flowDefinitionMapper.selectList(
                new LambdaQueryWrapper<FlowDefinition>().eq(FlowDefinition::getFlowCode, instanceBo.getFlowCode()));
            if (CollUtil.isNotEmpty(flowDefinitions)) {
                List<Long> defIdList = StreamUtils.toList(flowDefinitions, FlowDefinition::getId);
                wrapper.in(FlowInstance::getDefinitionId, defIdList);
            }
        }
        wrapper.eq(FlowInstance::getCreateBy, LoginHelper.getUserId());
        Page<FlowInstance> page = flowInstanceMapper.selectPage(pageQuery.build(), wrapper);
        TableDataInfo<FlowInstanceVo> build = TableDataInfo.build();
        List<FlowInstanceVo> flowInstanceVos = BeanUtil.copyToList(page.getRecords(), FlowInstanceVo.class);
        if (CollUtil.isNotEmpty(flowInstanceVos)) {
            List<Long> definitionIds = StreamUtils.toList(flowInstanceVos, FlowInstanceVo::getDefinitionId);
            List<FlowDefinition> flowDefinitions = flowDefinitionMapper.selectByIds(definitionIds);
            for (FlowInstanceVo vo : flowInstanceVos) {
                flowDefinitions.stream().filter(e -> e.getId().toString().equals(vo.getDefinitionId().toString())).findFirst().ifPresent(e -> {
                    vo.setFlowName(e.getFlowName());
                    vo.setFlowCode(e.getFlowCode());
                    vo.setVersion(e.getVersion());
                    vo.setFlowStatusName(FlowStatus.getValueByKey(vo.getFlowStatus()));
                });
            }

        }
        build.setRows(flowInstanceVos);
        build.setTotal(page.getTotal());
        return build;
    }

    /**
     * 获取流程图,流程记录
     *
     * @param businessId 业务id
     */
    @Override
    public Map<String, Object> getFlowImage(String businessId) {
        Map<String, Object> map = new HashMap<>(16);
        FlowInstance flowInstance = instanceByBusinessId(businessId);
        LambdaQueryWrapper<FlowHisTask> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(FlowHisTask::getInstanceId, flowInstance.getId());
        wrapper.eq(FlowHisTask::getNodeType, NodeType.BETWEEN.getKey());
        wrapper.orderByDesc(FlowHisTask::getCreateTime);
        List<FlowHisTask> flowHisTasks = flowHisTaskMapper.selectList(wrapper);
        List<FlowHisTaskVo> list = BeanUtil.copyToList(flowHisTasks, FlowHisTaskVo.class);
        for (FlowHisTaskVo vo : list) {
            vo.setCooperateTypeName(CooperateType.getValueByKey(vo.getCooperateType()));
            if (vo.getUpdateTime() != null && vo.getCreateTime() != null) {
                vo.setRunDuration(getDuration(vo.getUpdateTime().getTime() - vo.getCreateTime().getTime()));
            }
        }
        map.put("list", list);
        try {
            String flowChart = defService.flowChart(flowInstance.getId());
            map.put("image", flowChart);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return map;
    }

    /**
     * 任务完成时间处理
     *
     * @param time 时间
     */
    private String getDuration(long time) {

        long day = time / (24 * 60 * 60 * 1000);
        long hour = (time / (60 * 60 * 1000) - day * 24);
        long minute = ((time / (60 * 1000)) - day * 24 * 60 - hour * 60);
        long second = (time / 1000 - day * 24 * 60 * 60 - hour * 60 * 60 - minute * 60);

        if (day > 0) {
            return day + "天" + hour + "小时" + minute + "分钟";
        }
        if (hour > 0) {
            return hour + "小时" + minute + "分钟";
        }
        if (minute > 0) {
            return minute + "分钟";
        }
        if (second > 0) {
            return second + "秒";
        } else {
            return 0 + "秒";
        }
    }

    /**
     * 按照实例id更新状态
     *
     * @param instanceId 实例id
     * @param status     状态
     */
    @Override
    public void updateStatus(Long instanceId, String status) {
        LambdaUpdateWrapper<FlowInstance> wrapper = new LambdaUpdateWrapper<>();
        wrapper.set(FlowInstance::getFlowStatus, status);
        wrapper.eq(FlowInstance::getId, instanceId);
        flowInstanceMapper.update(wrapper);
    }
}
