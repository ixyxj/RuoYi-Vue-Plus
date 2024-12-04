package org.dromara.workflow.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.dto.UserDTO;
import org.dromara.common.core.enums.BusinessStatusEnum;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.service.AssigneeService;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.warm.flow.core.dto.FlowParams;
import org.dromara.warm.flow.core.entity.*;
import org.dromara.warm.flow.core.enums.NodeType;
import org.dromara.warm.flow.core.enums.SkipType;
import org.dromara.warm.flow.core.enums.UserType;
import org.dromara.warm.flow.core.service.*;
import org.dromara.warm.flow.orm.entity.*;
import org.dromara.warm.flow.orm.mapper.FlowHisTaskMapper;
import org.dromara.warm.flow.orm.mapper.FlowSkipMapper;
import org.dromara.warm.flow.orm.mapper.FlowTaskMapper;
import org.dromara.workflow.common.enums.TaskAssigneeType;
import org.dromara.workflow.common.enums.TaskStatusEnum;
import org.dromara.workflow.domain.bo.*;
import org.dromara.workflow.domain.vo.FlowHisTaskVo;
import org.dromara.workflow.domain.vo.FlowTaskVo;
import org.dromara.workflow.domain.vo.WfCopy;
import org.dromara.workflow.domain.vo.WfDefinitionConfigVo;
import org.dromara.workflow.handler.FlowProcessEventHandler;
import org.dromara.workflow.handler.WorkflowPermissionHandler;
import org.dromara.workflow.mapper.FlwTaskMapper;
import org.dromara.workflow.service.IFlwInstanceService;
import org.dromara.workflow.service.IFlwTaskService;
import org.dromara.workflow.service.IWfDefinitionConfigService;
import org.dromara.workflow.utils.WorkflowUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static org.dromara.workflow.common.constant.FlowConstant.*;

/**
 * 任务 服务层实现
 *
 * @author may
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class FlwTaskServiceImpl implements IFlwTaskService, AssigneeService {

    private final TaskService taskService;
    private final InsService insService;
    private final FlwTaskMapper flwTaskMapper;
    private final UserService userService;
    private final IWfDefinitionConfigService wfDefinitionConfigService;
    private final IFlwInstanceService iFlwInstanceService;
    private final FlowTaskMapper flowTaskMapper;
    private final FlowHisTaskMapper flowHisTaskMapper;
    private final FlowSkipMapper flowSkipMapper;
    private final FlowProcessEventHandler flowProcessEventHandler;
    private final DefService defService;
    private final HisTaskService hisTaskService;
    private final IdentifierGenerator identifierGenerator;

    /**
     * 启动任务
     *
     * @param startProcessBo 启动流程参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> startWorkFlow(StartProcessBo startProcessBo) {
        String businessKey = startProcessBo.getBusinessKey();
        if (StringUtils.isBlank(businessKey)) {
            throw new ServiceException("启动工作流时必须包含业务ID");
        }
        // 启动流程实例（提交申请）
        Map<String, Object> variables = startProcessBo.getVariables();
        // 流程发起人
        variables.put(INITIATOR, LoginHelper.getUserIdStr());
        // 业务id
        variables.put(BUSINESS_KEY, businessKey);
        WfDefinitionConfigVo wfDefinitionConfigVo = wfDefinitionConfigService.getByTableNameLastVersion(startProcessBo.getTableName());
        if (wfDefinitionConfigVo == null || wfDefinitionConfigVo.getDefinitionId() == null) {
            throw new ServiceException("请到流程定义绑定业务表名与流程KEY！");
        }
        Long definitionId = wfDefinitionConfigVo.getDefinitionId();
        Definition definition = defService.getById(definitionId);
        if (definition == null) {
            log.error("流程定义ID【{}】不存在！", definitionId);
            throw new ServiceException("请到流程定义ID【" + definitionId + "】不存在！");
        }
        FlowInstance flowInstance = iFlwInstanceService.instanceByBusinessId(businessKey);
        if (flowInstance != null) {
            List<Task> taskList = taskService.list(new FlowTask().setInstanceId(flowInstance.getId()));
            return Map.of(PROCESS_INSTANCE_ID, taskList.get(0).getInstanceId(), "taskId", taskList.get(0).getId());
        }
        FlowParams flowParams = new FlowParams();
        flowParams.flowCode(wfDefinitionConfigVo.getProcessKey());
        flowParams.variable(startProcessBo.getVariables());
        flowParams.flowStatus(BusinessStatusEnum.DRAFT.getStatus());
        Instance instance;
        try {
            instance = insService.start(businessKey, flowParams);
        } catch (Exception e) {
            throw new ServiceException(e.getMessage());
        }
        // 申请人执行流程
        List<Task> taskList = taskService.list(new FlowTask().setInstanceId(instance.getId()));
        if (taskList.size() > 1) {
            throw new ServiceException("请检查流程第一个环节是否为申请人！");
        }
        return Map.of("processInstanceId", instance.getId(), "taskId", taskList.get(0).getId());
    }

    /**
     * 办理任务
     *
     * @param completeTaskBo 办理任务参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean completeTask(CompleteTaskBo completeTaskBo) {
        try {
            // 获取任务ID并查询对应的流程任务和实例信息
            Long taskId = completeTaskBo.getTaskId();
            // 获取抄送人
            List<WfCopy> wfCopyList = completeTaskBo.getWfCopyList();
            FlowTask flowTask = flowTaskMapper.selectById(taskId);
            Instance ins = insService.getById(flowTask.getInstanceId());

            // 获取流程定义信息
            Definition definition = defService.getById(flowTask.getDefinitionId());

            // 检查流程状态是否为草稿、已撤销或已退回状态，若是则执行流程提交监听
            if (BusinessStatusEnum.isDraftOrCancelOrBack(ins.getFlowStatus())) {
                flowProcessEventHandler.processHandler(definition.getFlowCode(), ins.getBusinessId(), ins.getFlowStatus(), true);
            }

            // 办理任务监听，记录任务执行信息
            flowProcessEventHandler.processTaskHandler(definition.getFlowCode(), flowTask.getNodeCode(), taskId.toString(), ins.getBusinessId());

            // 构建流程参数，包括变量、跳转类型、消息、处理人、权限等信息
            FlowParams flowParams = new FlowParams();
            flowParams.variable(completeTaskBo.getVariables());
            flowParams.skipType(SkipType.PASS.getKey());
            flowParams.message(completeTaskBo.getMessage());
            flowParams.flowStatus(BusinessStatusEnum.WAITING.getStatus()).hisStatus(TaskStatusEnum.PASS.getStatus());
            // 执行任务跳转，并根据返回的处理人设置下一步处理人
            setHandler(taskService.skip(taskId, flowParams), flowTask, wfCopyList);

            // 更新实例状态为待审核状态
            iFlwInstanceService.updateStatus(ins.getId(), BusinessStatusEnum.WAITING.getStatus());
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * 设置办理人
     *
     * @param instance   实例
     * @param task       (当前任务)未办理的任务
     * @param wfCopyList 抄送人
     */
    private void setHandler(Instance instance, FlowTask task, List<WfCopy> wfCopyList) {
        if (instance == null) {
            return;
        }
        //添加抄送人
        setCopy(task, wfCopyList);
        // 根据流程实例ID查询所有关联的任务
        List<FlowTask> flowTasks = flowTaskMapper.selectList(new LambdaQueryWrapper<>(FlowTask.class)
            .eq(FlowTask::getInstanceId, instance.getId()));
        List<User> userList = new ArrayList<>();
        // 遍历任务列表，处理每个任务的办理人
        for (FlowTask flowTask : flowTasks) {
            // 获取与当前任务关联的用户列表
            List<User> associatedUsers = userService.getByAssociateds(Collections.singletonList(flowTask.getId()));
            if (CollUtil.isNotEmpty(associatedUsers)) {
                userList.addAll(WorkflowUtils.getUser(associatedUsers, flowTask.getId()));
            }
        }
        // 批量删除现有任务的办理人记录
        userService.deleteByTaskIds(StreamUtils.toList(flowTasks, FlowTask::getId));
        // 确保要保存的 userList 不为空
        if (CollUtil.isNotEmpty(userList)) {
            userService.saveBatch(userList);
        }
    }

    /**
     * 添加抄送人
     *
     * @param task       任务信息
     * @param wfCopyList 抄送人
     */
    private void setCopy(FlowTask task, List<WfCopy> wfCopyList) {
        // 添加抄送人记录
        if (CollUtil.isNotEmpty(wfCopyList)) {
            FlowHisTask flowHisTask = flowHisTaskMapper.selectOne(new LambdaQueryWrapper<>(FlowHisTask.class).eq(FlowHisTask::getTaskId, task.getId()));
            FlowNode flowNode = new FlowNode();
            flowNode.setNodeCode(flowHisTask.getTargetNodeCode());
            flowNode.setNodeName(flowHisTask.getTargetNodeName());
            //生成新的任务id
            long taskId = identifierGenerator.nextId(null).longValue();
            task.setId(taskId);
            task.setNodeName("【抄送】" + task.getNodeName());
            Date updateTime = new Date(flowHisTask.getUpdateTime().getTime()-1000);
            FlowParams flowParams = FlowParams.build();
            flowParams.skipType(SkipType.NONE.getKey());
            flowParams.hisStatus(TaskStatusEnum.COPY.getStatus());
            flowParams.message("【抄送给】" + StreamUtils.join(wfCopyList, WfCopy::getUserName));
            HisTask hisTask = hisTaskService.setSkipHisTask(task, flowNode, flowParams);
            hisTask.setCreateTime(updateTime);
            hisTask.setUpdateTime(updateTime);
            hisTaskService.save(hisTask);
            //保存抄送人员
            List<User> userList = new ArrayList<>();
            for (WfCopy wfCopy : wfCopyList) {
                FlowUser flowUser = new FlowUser();
                flowUser.setType(TaskAssigneeType.COPY.getCode());
                flowUser.setProcessedBy(wfCopy.getUserId());
                flowUser.setAssociated(taskId);
                userList.add(flowUser);
            }
            if (CollUtil.isNotEmpty(userList)) {
                userService.saveBatch(userList);
            }
        }
    }

    /**
     * 查询当前用户的待办任务
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     */
    @Override
    public TableDataInfo<FlowTaskVo> getPageByTaskWait(FlowTaskBo flowTaskBo, PageQuery pageQuery) {
        QueryWrapper<FlowTaskBo> queryWrapper = buildQueryWrapper(flowTaskBo);
        queryWrapper.eq("t.node_type", NodeType.BETWEEN.getKey());
        queryWrapper.in("t.processed_by", SpringUtils.getBean(WorkflowPermissionHandler.class).permissions());
        queryWrapper.in("t.flow_status", BusinessStatusEnum.WAITING.getStatus());
        Page<FlowTaskVo> page = flwTaskMapper.getTaskWaitByPage(pageQuery.build(), queryWrapper);
        return TableDataInfo.build(page);
    }

    /**
     * 查询当前用户的已办任务
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     */
    @Override
    public TableDataInfo<FlowHisTaskVo> getPageByTaskFinish(FlowTaskBo flowTaskBo, PageQuery pageQuery) {
        QueryWrapper<FlowTaskBo> queryWrapper = buildQueryWrapper(flowTaskBo);
        queryWrapper.eq("t.node_type", NodeType.BETWEEN.getKey());
        queryWrapper.in("t.approver", LoginHelper.getUserIdStr());
        Page<FlowHisTaskVo> page = flwTaskMapper.getTaskFinishByPage(pageQuery.build(), queryWrapper);
        return TableDataInfo.build(page);
    }

    /**
     * 查询待办任务
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     */
    @Override
    public TableDataInfo<FlowTaskVo> getPageByAllTaskWait(FlowTaskBo flowTaskBo, PageQuery pageQuery) {
        QueryWrapper<FlowTaskBo> queryWrapper = buildQueryWrapper(flowTaskBo);
        queryWrapper.eq("t.node_type", NodeType.BETWEEN.getKey());
        Page<FlowTaskVo> page = flwTaskMapper.getTaskWaitByPage(pageQuery.build(), queryWrapper);
        return TableDataInfo.build(page);
    }

    /**
     * 查询已办任务
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     */
    @Override
    public TableDataInfo<FlowHisTaskVo> getPageByAllTaskFinish(FlowTaskBo flowTaskBo, PageQuery pageQuery) {
        QueryWrapper<FlowTaskBo> queryWrapper = buildQueryWrapper(flowTaskBo);
        Page<FlowHisTaskVo> page = flwTaskMapper.getTaskFinishByPage(pageQuery.build(), queryWrapper);
        return TableDataInfo.build(page);
    }

    /**
     * 查询当前用户的抄送
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     */
    @Override
    public TableDataInfo<FlowTaskVo> getPageByTaskCopy(FlowTaskBo flowTaskBo, PageQuery pageQuery) {
        QueryWrapper<FlowTaskBo> queryWrapper = buildQueryWrapper(flowTaskBo);
        queryWrapper.in("t.processed_by", LoginHelper.getUserIdStr());
        Page<FlowTaskVo> page = flwTaskMapper.getTaskCopyByPage(pageQuery.build(), queryWrapper);
        return TableDataInfo.build(page);
    }

    private QueryWrapper<FlowTaskBo> buildQueryWrapper(FlowTaskBo flowTaskBo) {
        QueryWrapper<FlowTaskBo> wrapper = Wrappers.query();
        wrapper.like(StringUtils.isNotBlank(flowTaskBo.getNodeName()), "t.node_name", flowTaskBo.getNodeName());
        wrapper.like(StringUtils.isNotBlank(flowTaskBo.getFlowName()), "t.flow_name", flowTaskBo.getFlowName());
        wrapper.eq(StringUtils.isNotBlank(flowTaskBo.getFlowCode()), "t.flow_code", flowTaskBo.getFlowCode());
        wrapper.orderByDesc("t.create_time");
        return wrapper;
    }

    /**
     * 驳回任务
     *
     * @param bo 参数
     */
    @Override
    public boolean backProcess(BackProcessBo bo) {
        try {
            Long taskId = bo.getTaskId();
            List<FlowTask> flowTasks = flowTaskMapper.selectList(new LambdaQueryWrapper<>(FlowTask.class).eq(FlowTask::getId, taskId));
            if (CollUtil.isEmpty(flowTasks)) {
                throw new ServiceException("任务不存在！");
            }
            Long definitionId = flowTasks.get(0).getDefinitionId();
            Definition definition = defService.getById(definitionId);
            List<FlowSkip> flowSkips = flowSkipMapper.selectList(new LambdaQueryWrapper<>(FlowSkip.class).eq(FlowSkip::getDefinitionId, definitionId));
            FlowSkip flowSkip = StreamUtils.findFirst(flowSkips, e -> NodeType.START.getKey().equals(e.getNowNodeType()));
            //开始节点的下一节点
            assert flowSkip != null;
            String nextNodeCode = flowSkip.getNextNodeCode();

            FlowParams flowParams = new FlowParams();
            flowParams.variable(bo.getVariables());
            flowParams.skipType(SkipType.PASS.getKey());
            if (nextNodeCode.equals(bo.getNodeCode())) {
                flowParams.flowStatus(BusinessStatusEnum.BACK.getStatus());
            } else {
                flowParams.flowStatus(BusinessStatusEnum.WAITING.getStatus());
            }
            flowParams.hisStatus(TaskStatusEnum.BACK.getStatus());
            flowParams.message(bo.getMessage());
            flowParams.nodeCode(bo.getNodeCode());
            Instance instance = taskService.skip(taskId, flowParams);
            setHandler(instance, flowTasks.get(0), null);
            flowProcessEventHandler.processHandler(definition.getFlowCode(),
                instance.getBusinessId(), BusinessStatusEnum.BACK.getStatus(), false);
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * 获取可驳回节点
     *
     * @param instanceId 实例id
     */
    @Override
    public List<HisTask> getBackTaskNode(String instanceId) {
        LambdaQueryWrapper<FlowHisTask> lw = new LambdaQueryWrapper<>(FlowHisTask.class)
            .eq(FlowHisTask::getInstanceId, instanceId)
            .eq(FlowHisTask::getNodeType, NodeType.BETWEEN.getKey())
            .orderByDesc(FlowHisTask::getCreateTime);
        List<FlowHisTask> flowHisTasks = flowHisTaskMapper.selectList(lw);
        if (CollUtil.isNotEmpty(flowHisTasks)) {
            return flowHisTasks.stream().distinct().collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * 终止任务
     *
     * @param bo 参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean terminationTask(FlowTerminationBo bo) {
        try {
            FlowTask flowTask = flowTaskMapper.selectById(bo.getTaskId());
            Instance ins = insService.getById(flowTask.getInstanceId());
            //流程定义
            Definition definition = defService.getById(flowTask.getDefinitionId());
            FlowParams flowParams = new FlowParams();
            flowParams.message(bo.getComment());
            flowParams.flowStatus(BusinessStatusEnum.TERMINATION.getStatus())
                .hisStatus(TaskStatusEnum.TERMINATION.getStatus());
            taskService.termination(bo.getTaskId(), flowParams);
            //流程终止监听
            flowProcessEventHandler.processHandler(definition.getFlowCode(),
                ins.getBusinessId(), BusinessStatusEnum.TERMINATION.getStatus(), false);
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * 通过taskId查询对应的任务办理人
     *
     * @param taskIds taskId串逗号分隔
     * @return 任务办理人名称串逗号分隔
     */
    @Override
    public String selectAssigneeByIds(String taskIds) {
        if (StringUtils.isBlank(taskIds)) {
            return null;
        }
        List<User> userList = userService.getByAssociateds(List.of(Long.valueOf(taskIds))
            , UserType.APPROVAL.getKey(), UserType.TRANSFER.getKey(), UserType.DEPUTE.getKey());
        // 获取处理用户的昵称并将其合并为一个字符串
        return WorkflowUtils.getHandlerUser(userList).stream()
            .map(UserDTO::getNickName)
            .collect(Collectors.joining(","));
    }

    /**
     * 通过taskId查询对应的任务办理人列表
     *
     * @param taskIdList 任务id
     * @return 列表
     */
    @Override
    public List<UserDTO> selectByIds(List<Long> taskIdList) {
        if (CollUtil.isEmpty(taskIdList)) {
            return Collections.emptyList();
        }
        List<User> userList = userService.getByAssociateds(taskIdList
            , UserType.APPROVAL.getKey(), UserType.TRANSFER.getKey(), UserType.DEPUTE.getKey());
        return WorkflowUtils.getHandlerUser(userList);
    }

}
