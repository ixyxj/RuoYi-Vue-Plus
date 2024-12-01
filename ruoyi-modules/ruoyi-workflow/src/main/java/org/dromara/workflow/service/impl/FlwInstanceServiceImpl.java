package org.dromara.workflow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.enums.BusinessStatusEnum;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.warm.flow.core.constant.ExceptionCons;
import org.dromara.warm.flow.core.dto.FlowParams;
import org.dromara.warm.flow.core.entity.Definition;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.core.entity.Node;
import org.dromara.warm.flow.core.enums.CooperateType;
import org.dromara.warm.flow.core.enums.FlowStatus;
import org.dromara.warm.flow.core.enums.NodeType;
import org.dromara.warm.flow.core.enums.SkipType;
import org.dromara.warm.flow.core.service.DefService;
import org.dromara.warm.flow.core.service.InsService;
import org.dromara.warm.flow.core.service.NodeService;
import org.dromara.warm.flow.core.service.TaskService;
import org.dromara.warm.flow.core.utils.AssertUtil;
import org.dromara.warm.flow.orm.entity.FlowDefinition;
import org.dromara.warm.flow.orm.entity.FlowHisTask;
import org.dromara.warm.flow.orm.entity.FlowInstance;
import org.dromara.warm.flow.orm.entity.FlowNode;
import org.dromara.warm.flow.orm.mapper.FlowDefinitionMapper;
import org.dromara.warm.flow.orm.mapper.FlowHisTaskMapper;
import org.dromara.warm.flow.orm.mapper.FlowInstanceMapper;
import org.dromara.warm.flow.orm.mapper.FlowNodeMapper;
import org.dromara.workflow.common.enums.TaskStatusEnum;
import org.dromara.workflow.domain.bo.FlowCancelBo;
import org.dromara.workflow.domain.bo.FlowInstanceBo;
import org.dromara.workflow.domain.vo.FlowHisTaskVo;
import org.dromara.workflow.domain.vo.FlowInstanceVo;
import org.dromara.workflow.domain.vo.VariableVo;
import org.dromara.workflow.handler.FlowProcessEventHandler;
import org.dromara.workflow.mapper.FlwInstanceMapper;
import org.dromara.workflow.service.IFlwInstanceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;

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
    private final TaskService taskService;
    private final FlowNodeMapper flowNodeMapper;
    private final NodeService nodeService;
    private final FlowProcessEventHandler flowProcessEventHandler;

    /**
     * 分页查询正在运行的流程实例
     *
     * @param instance  参数
     * @param pageQuery 分页
     */
    @Override
    public TableDataInfo<FlowInstanceVo> getPageByRunning(Instance instance, PageQuery pageQuery) {
        QueryWrapper<FlowInstanceBo> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("t.flow_status", BusinessStatusEnum.runningStatus());
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
        queryWrapper.in("t.flow_status", BusinessStatusEnum.finishStatus());
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
     * @param bo 参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelProcessApply(FlowCancelBo bo) {
        try {
            Instance instance = instanceByBusinessId(bo.getBusinessId());
            if (instance == null) {
                throw new ServiceException(ExceptionCons.NOT_FOUNT_INSTANCE);
            }
            Definition definition = defService.getById(instance.getDefinitionId());
            if (definition == null) {
                throw new ServiceException(ExceptionCons.NOT_FOUNT_DEF);
            }
            //获取已发布的流程节点
            List<FlowNode> flowNodes = flowNodeMapper.selectList(new LambdaQueryWrapper<FlowNode>().eq(FlowNode::getDefinitionId, definition.getId()));
            AssertUtil.isTrue(CollUtil.isEmpty(flowNodes), ExceptionCons.NOT_PUBLISH_NODE);
            Node startNode = flowNodes.stream().filter(t -> NodeType.isStart(t.getNodeType())).findFirst().orElse(null);
            AssertUtil.isNull(startNode, ExceptionCons.LOST_START_NODE);
            Node nextNode = nodeService.getNextNode(definition.getId(), startNode.getNodeCode(), null, SkipType.NONE.getKey());
            FlowParams flowParams = FlowParams.build();
            flowParams.handler(LoginHelper.getUserIdStr());
            flowParams.nodeCode(nextNode.getNodeCode());
            flowParams.message(bo.getMessage());
            flowParams.flowStatus(BusinessStatusEnum.CANCEL.getStatus()).hisStatus(TaskStatusEnum.CANCEL.getStatus());
            taskService.retrieve(instance.getId(), flowParams);
            // 更新状态
            updateStatus(instance.getId(), BusinessStatusEnum.CANCEL.getStatus());
            //流程撤销监听
            flowProcessEventHandler.processHandler(definition.getFlowCode(),
                bo.getBusinessId(), BusinessStatusEnum.CANCEL.getStatus(), false);
        } catch (Exception e) {
            log.error("撤销失败: {}", e.getMessage(), e);
            throw new ServiceException(e.getMessage());
        }
        return true;
    }

    /**
     * 获取当前登陆人发起的流程实例
     *
     * @param instanceBo 参数
     * @param pageQuery  分页
     */
    @Override
    public TableDataInfo<FlowInstanceVo> getPageByCurrent(FlowInstanceBo instanceBo, PageQuery pageQuery) {
        LambdaQueryWrapper<FlowInstance> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.isNotBlank(instanceBo.getFlowCode())) {
            List<FlowDefinition> flowDefinitions = flowDefinitionMapper.selectList(
                new LambdaQueryWrapper<FlowDefinition>().eq(FlowDefinition::getFlowCode, instanceBo.getFlowCode()));
            if (CollUtil.isNotEmpty(flowDefinitions)) {
                List<Long> defIdList = StreamUtils.toList(flowDefinitions, FlowDefinition::getId);
                wrapper.in(FlowInstance::getDefinitionId, defIdList);
            }
        }
        wrapper.eq(FlowInstance::getCreateBy, LoginHelper.getUserIdStr());
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
        wrapper.orderByDesc(FlowHisTask::getCreateTime).orderByDesc(FlowHisTask::getUpdateTime);
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

    /**
     * 获取流程变量
     *
     * @param instanceId 实例id
     */
    @Override
    public Map<String, Object> getInstanceVariable(String instanceId) {
        Map<String, Object> map = new HashMap<>();
        FlowInstance flowInstance = flowInstanceMapper.selectById(instanceId);
        Map<String, Object> variableMap = flowInstance.getVariableMap();
        List<VariableVo> list = new ArrayList<>();
        if (CollUtil.isNotEmpty(variableMap)) {
            for (Map.Entry<String, Object> entry : variableMap.entrySet()) {
                VariableVo variableVo = new VariableVo();
                variableVo.setKey(entry.getKey());
                variableVo.setValue(entry.getValue().toString());
                list.add(variableVo);
            }
        }
        map.put("variableList", list);
        map.put("variable", flowInstance.getVariable());
        return map;
    }
}
