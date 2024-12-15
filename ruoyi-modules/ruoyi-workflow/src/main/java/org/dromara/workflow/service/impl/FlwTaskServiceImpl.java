package org.dromara.workflow.service.impl;

import cn.hutool.core.bean.BeanUtil;
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
import org.dromara.common.core.service.UserService;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.core.utils.ValidatorUtils;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
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
import org.dromara.warm.flow.orm.mapper.FlowInstanceMapper;
import org.dromara.warm.flow.orm.mapper.FlowSkipMapper;
import org.dromara.warm.flow.orm.mapper.FlowTaskMapper;
import org.dromara.workflow.common.enums.TaskAssigneeType;
import org.dromara.workflow.common.enums.TaskStatusEnum;
import org.dromara.workflow.domain.bo.*;
import org.dromara.workflow.domain.vo.FlowHisTaskVo;
import org.dromara.workflow.domain.vo.FlowTaskVo;
import org.dromara.workflow.domain.vo.FlowCopy;
import org.dromara.workflow.handler.FlowProcessEventHandler;
import org.dromara.workflow.handler.WorkflowPermissionHandler;
import org.dromara.workflow.mapper.FlwTaskMapper;
import org.dromara.workflow.service.IFlwTaskService;
import org.dromara.workflow.utils.WorkflowUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
public class FlwTaskServiceImpl implements IFlwTaskService {

    private final TaskService taskService;
    private final InsService insService;
    private final FlowInstanceMapper flowInstanceMapper;
    private final FlwTaskMapper flwTaskMapper;
    private final UserService userService;
    private final FlowTaskMapper flowTaskMapper;
    private final FlowHisTaskMapper flowHisTaskMapper;
    private final FlowSkipMapper flowSkipMapper;
    private final FlowProcessEventHandler flowProcessEventHandler;
    private final DefService defService;
    private final HisTaskService hisTaskService;
    private final IdentifierGenerator identifierGenerator;
    private final NodeService nodeService;

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
        FlowInstance flowInstance = flowInstanceMapper.selectOne(new LambdaQueryWrapper<>(FlowInstance.class)
            .eq(FlowInstance::getBusinessId, businessKey));
        if (flowInstance != null) {
            BusinessStatusEnum.checkStartStatus(flowInstance.getFlowStatus());
            List<Task> taskList = taskService.list(new FlowTask().setInstanceId(flowInstance.getId()));
            return Map.of(PROCESS_INSTANCE_ID, taskList.get(0).getInstanceId(), TASK_ID, taskList.get(0).getId());
        }
        FlowParams flowParams = new FlowParams();
        flowParams.flowCode(startProcessBo.getFlowCode());
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
        return Map.of(PROCESS_INSTANCE_ID, instance.getId(), TASK_ID, taskList.get(0).getId());
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
            List<String> messageType = completeTaskBo.getMessageType();
            String notice = completeTaskBo.getNotice();
            // 获取抄送人
            List<FlowCopy> flowCopyList = completeTaskBo.getFlowCopyList();
            FlowTask flowTask = flowTaskMapper.selectById(taskId);
            Instance ins = insService.getById(flowTask.getInstanceId());
            // 获取流程定义信息
            Definition definition = defService.getById(flowTask.getDefinitionId());
            // 检查流程状态是否为草稿、已撤销或已退回状态，若是则执行流程提交监听
            if (BusinessStatusEnum.isDraftOrCancelOrBack(ins.getFlowStatus())) {
                flowProcessEventHandler.processHandler(definition.getFlowCode(), ins.getBusinessId(), ins.getFlowStatus(), true);
            }
            // 构建流程参数，包括变量、跳转类型、消息、处理人、权限等信息
            FlowParams flowParams = new FlowParams();
            flowParams.variable(completeTaskBo.getVariables());
            flowParams.skipType(SkipType.PASS.getKey());
            flowParams.message(completeTaskBo.getMessage());
            flowParams.flowStatus(BusinessStatusEnum.WAITING.getStatus()).hisStatus(TaskStatusEnum.PASS.getStatus());

            flowParams.hisTaskExt(completeTaskBo.getFileId());
            // 执行任务跳转，并根据返回的处理人设置下一步处理人
            setHandler(taskService.skip(taskId, flowParams), flowTask, flowCopyList);
            // 消息通知
            WorkflowUtils.sendMessage(definition.getFlowName(), ins.getId(), messageType, notice);
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * 设置办理人
     *
     * @param instance     实例
     * @param task         (当前任务)未办理的任务
     * @param flowCopyList 抄送人
     */
    private void setHandler(Instance instance, FlowTask task, List<FlowCopy> flowCopyList) {
        if (instance == null) {
            return;
        }
        //添加抄送人
        setCopy(task, flowCopyList);
        // 根据流程实例ID查询所有关联的任务
        List<FlowTask> flowTasks = selectByInstId(instance.getId());
        List<User> userList = new ArrayList<>();
        // 遍历任务列表，处理每个任务的办理人
        for (FlowTask flowTask : flowTasks) {
            // 获取与当前任务关联的用户列表
            List<User> associatedUsers = WorkflowUtils.getFlowUserService().getByAssociateds(Collections.singletonList(flowTask.getId()));
            if (CollUtil.isNotEmpty(associatedUsers)) {
                userList.addAll(WorkflowUtils.buildUser(associatedUsers, flowTask.getId()));
            }
        }
        // 批量删除现有任务的办理人记录
        if (CollUtil.isNotEmpty(flowTasks)) {
            WorkflowUtils.getFlowUserService().deleteByTaskIds(StreamUtils.toList(flowTasks, FlowTask::getId));
        }
        // 确保要保存的 userList 不为空
        if (CollUtil.isNotEmpty(userList)) {
            WorkflowUtils.getFlowUserService().saveBatch(userList);
        }
    }

    /**
     * 添加抄送人
     *
     * @param task         任务信息
     * @param flowCopyList 抄送人
     */
    private void setCopy(FlowTask task, List<FlowCopy> flowCopyList) {
        if (CollUtil.isEmpty(flowCopyList)) {
            return;
        }
        // 添加抄送人记录
        FlowHisTask flowHisTask = flowHisTaskMapper.selectOne(new LambdaQueryWrapper<>(FlowHisTask.class).eq(FlowHisTask::getTaskId, task.getId()));
        FlowNode flowNode = new FlowNode();
        flowNode.setNodeCode(flowHisTask.getTargetNodeCode());
        flowNode.setNodeName(flowHisTask.getTargetNodeName());
        //生成新的任务id
        long taskId = identifierGenerator.nextId(null).longValue();
        task.setId(taskId);
        task.setNodeName("【抄送】" + task.getNodeName());
        Date updateTime = new Date(flowHisTask.getUpdateTime().getTime() - 1000);
        FlowParams flowParams = FlowParams.build();
        flowParams.skipType(SkipType.NONE.getKey());
        flowParams.hisStatus(TaskStatusEnum.COPY.getStatus());
        flowParams.message("【抄送给】" + StreamUtils.join(flowCopyList, FlowCopy::getUserName));
        HisTask hisTask = hisTaskService.setSkipHisTask(task, flowNode, flowParams);
        hisTask.setCreateTime(updateTime);
        hisTask.setUpdateTime(updateTime);
        hisTaskService.save(hisTask);
        List<User> userList = flowCopyList.stream()
            .map(flowCopy -> {
                FlowUser flowUser = new FlowUser();
                flowUser.setType(TaskAssigneeType.COPY.getCode());
                flowUser.setProcessedBy(String.valueOf(flowCopy.getUserId()));
                flowUser.setAssociated(taskId);
                return flowUser;
            }).collect(Collectors.toList());
        // 批量保存抄送人员
        WorkflowUtils.getFlowUserService().saveBatch(userList);
    }

    /**
     * 查询当前用户的待办任务
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     */
    @Override
    public TableDataInfo<FlowTaskVo> pageByTaskWait(FlowTaskBo flowTaskBo, PageQuery pageQuery) {
        QueryWrapper<FlowTaskBo> queryWrapper = buildQueryWrapper(flowTaskBo);
        queryWrapper.eq("t.node_type", NodeType.BETWEEN.getKey());
        queryWrapper.in("t.processed_by", SpringUtils.getBean(WorkflowPermissionHandler.class).permissions());
        queryWrapper.in("t.flow_status", BusinessStatusEnum.WAITING.getStatus());
        Page<FlowTaskVo> page = getFlowTaskVoPage(pageQuery, queryWrapper);
        return TableDataInfo.build(page);
    }

    /**
     * 查询当前用户的已办任务
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     */
    @Override
    public TableDataInfo<FlowHisTaskVo> pageByTaskFinish(FlowTaskBo flowTaskBo, PageQuery pageQuery) {
        QueryWrapper<FlowTaskBo> queryWrapper = buildQueryWrapper(flowTaskBo);
        queryWrapper.eq("t.node_type", NodeType.BETWEEN.getKey());
        queryWrapper.in("t.approver", LoginHelper.getUserIdStr());
        queryWrapper.orderByDesc("t.create_time").orderByDesc("t.update_time");
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
    public TableDataInfo<FlowTaskVo> pageByAllTaskWait(FlowTaskBo flowTaskBo, PageQuery pageQuery) {
        QueryWrapper<FlowTaskBo> queryWrapper = buildQueryWrapper(flowTaskBo);
        queryWrapper.eq("t.node_type", NodeType.BETWEEN.getKey());
        Page<FlowTaskVo> page = getFlowTaskVoPage(pageQuery, queryWrapper);
        return TableDataInfo.build(page);
    }

    private Page<FlowTaskVo> getFlowTaskVoPage(PageQuery pageQuery, QueryWrapper<FlowTaskBo> queryWrapper) {
        Page<FlowTaskVo> page = flwTaskMapper.getTaskWaitByPage(pageQuery.build(), queryWrapper);
        List<FlowTaskVo> records = page.getRecords();
        if (CollUtil.isNotEmpty(records)) {
            List<Long> taskIds = StreamUtils.toList(records, FlowTaskVo::getId);
            Map<Long, List<UserDTO>> listMap = currentTaskAllUser(taskIds);
            records.forEach(t -> {
                List<UserDTO> userList = listMap.getOrDefault(t.getId(), Collections.emptyList());
                if (CollUtil.isNotEmpty(userList)) {
                    t.setAssigneeIds(StreamUtils.join(userList, e -> String.valueOf(e.getUserId())));
                    t.setAssigneeNames(StreamUtils.join(userList, UserDTO::getNickName));
                }
            });
        }
        return page;
    }

    /**
     * 查询已办任务
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     */
    @Override
    public TableDataInfo<FlowHisTaskVo> pageByAllTaskFinish(FlowTaskBo flowTaskBo, PageQuery pageQuery) {
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
    public TableDataInfo<FlowTaskVo> pageByTaskCopy(FlowTaskBo flowTaskBo, PageQuery pageQuery) {
        QueryWrapper<FlowTaskBo> queryWrapper = buildQueryWrapper(flowTaskBo);
        queryWrapper.in("t.processed_by", LoginHelper.getUserIdStr());
        Page<FlowTaskVo> page = flwTaskMapper.getTaskCopyByPage(pageQuery.build(), queryWrapper);
        return TableDataInfo.build(page);
    }

    private QueryWrapper<FlowTaskBo> buildQueryWrapper(FlowTaskBo flowTaskBo) {
        QueryWrapper<FlowTaskBo> wrapper = Wrappers.query();
        wrapper.like(StringUtils.isNotBlank(flowTaskBo.getNodeName()), "t.node_name", flowTaskBo.getNodeName());
        wrapper.like(StringUtils.isNotBlank(flowTaskBo.getFlowName()), "t.flow_name", flowTaskBo.getFlowName());
        wrapper.like(StringUtils.isNotBlank(flowTaskBo.getFlowCode()), "t.flow_code", flowTaskBo.getFlowCode());
        wrapper.in(CollUtil.isNotEmpty(flowTaskBo.getCreateByIds()), "t.create_by", flowTaskBo.getCreateByIds());
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
            String notice = bo.getNotice();
            List<String> messageType = bo.getMessageType();
            String message = bo.getMessage();
            List<FlowTask> flowTasks = flowTaskMapper.selectList(new LambdaQueryWrapper<>(FlowTask.class).eq(FlowTask::getId, taskId));
            if (CollUtil.isEmpty(flowTasks)) {
                throw new ServiceException("任务不存在！");
            }
            Instance inst = insService.getById(flowTasks.get(0).getInstanceId());
            BusinessStatusEnum.checkBackStatus(inst.getFlowStatus());
            Long definitionId = flowTasks.get(0).getDefinitionId();
            Definition definition = defService.getById(definitionId);
            List<FlowSkip> flowSkips = flowSkipMapper.selectList(new LambdaQueryWrapper<>(FlowSkip.class).eq(FlowSkip::getDefinitionId, definitionId));
            FlowSkip flowSkip = StreamUtils.findFirst(flowSkips, e -> NodeType.START.getKey().equals(e.getNowNodeType()));
            //开始节点的下一节点
            assert flowSkip != null;
            //申请人节点
            String applyUserNodeCode = flowSkip.getNextNodeCode();
            if (applyUserNodeCode.equals(bo.getNodeCode())) {
                WorkflowUtils.backTask(message, inst.getId(), applyUserNodeCode, TaskStatusEnum.BACK.getStatus(), TaskStatusEnum.BACK.getStatus());
            } else {
                WorkflowUtils.backTask(message, inst.getId(), bo.getNodeCode(), TaskStatusEnum.WAITING.getStatus(), TaskStatusEnum.BACK.getStatus());
            }
            Instance instance = insService.getById(inst.getId());
            setHandler(instance, flowTasks.get(0), null);
            //消息通知
            WorkflowUtils.sendMessage(definition.getFlowName(), instance.getId(), messageType, notice);
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
    public List<FlowHisTask> getBackTaskNode(String instanceId) {
        // 创建查询条件，查询历史任务记录
        LambdaQueryWrapper<FlowHisTask> lw = new LambdaQueryWrapper<>(FlowHisTask.class)
            .select(FlowHisTask::getNodeCode, FlowHisTask::getNodeName)
            .eq(FlowHisTask::getInstanceId, instanceId)
            .eq(FlowHisTask::getNodeType, NodeType.BETWEEN.getKey())
            .orderByDesc(FlowHisTask::getCreateTime);

        List<FlowHisTask> flowHisTasks = flowHisTaskMapper.selectList(lw);
        if (CollUtil.isEmpty(flowHisTasks)) {
            return Collections.emptyList();
        }
        return new ArrayList<>(flowHisTasks.stream()
            .collect(Collectors.toMap(
                FlowHisTask::getNodeCode,
                task -> task,
                (existing, replacement) -> existing
            )).values());
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
            Long taskId = bo.getTaskId();
            Task task = taskService.getById(taskId);
            if (task == null) {
                throw new ServiceException("任务不存在！");
            }
            Instance instance = insService.getById(task.getInstanceId());
            if (instance != null) {
                BusinessStatusEnum.checkInvalidStatus(instance.getFlowStatus());
            }
            FlowParams flowParams = new FlowParams();
            flowParams.message(bo.getComment());
            flowParams.flowStatus(BusinessStatusEnum.TERMINATION.getStatus())
                .hisStatus(TaskStatusEnum.TERMINATION.getStatus());
            taskService.termination(taskId, flowParams);
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * 按照任务id查询任务
     *
     * @param taskIdList 任务id
     */
    @Override
    public List<FlowTask> selectByIdList(List<Long> taskIdList) {
        return flowTaskMapper.selectList(new LambdaQueryWrapper<>(FlowTask.class)
            .in(FlowTask::getId, taskIdList));
    }

    /**
     * 按照任务id查询任务
     *
     * @param taskId 任务id
     */
    @Override
    public FlowTaskVo selectById(Long taskId) {
        Task task = taskService.getById(taskId);
        if (task == null) {
            return null;
        }
        FlowTaskVo flowTaskVo = BeanUtil.toBean(task, FlowTaskVo.class);
        Instance instance = insService.getById(task.getInstanceId());
        Definition definition = defService.getById(task.getDefinitionId());
        flowTaskVo.setFlowStatus(instance.getFlowStatus());
        flowTaskVo.setVersion(definition.getVersion());
        flowTaskVo.setFlowCode(definition.getFlowCode());
        flowTaskVo.setFlowName(definition.getFlowName());
        flowTaskVo.setBusinessId(instance.getBusinessId());
        List<Node> nodeList = nodeService.getByNodeCodes(Collections.singletonList(flowTaskVo.getNodeCode()), instance.getDefinitionId());
        if (CollUtil.isNotEmpty(nodeList)) {
            Node node = nodeList.get(0);
            flowTaskVo.setNodeRatio(node.getNodeRatio());
        }
        return flowTaskVo;
    }

    /**
     * 按照任务id查询任务
     *
     * @param taskIdList 任务id
     * @return 结果
     */
    @Override
    public List<FlowHisTask> selectHisTaskByIdList(List<Long> taskIdList) {
        return flowHisTaskMapper.selectList(new LambdaQueryWrapper<>(FlowHisTask.class)
            .in(FlowHisTask::getId, taskIdList));
    }

    /**
     * 按照任务id查询任务
     *
     * @param taskId 任务id
     * @return 结果
     */
    @Override
    public FlowHisTask selectHisTaskById(Long taskId) {
        return flowHisTaskMapper.selectOne(new LambdaQueryWrapper<>(FlowHisTask.class)
            .eq(FlowHisTask::getId, taskId));
    }

    /**
     * 按照实例id查询任务
     *
     * @param instanceIdList 流程实例id
     */
    @Override
    public List<FlowTask> selectByInstIdList(List<Long> instanceIdList) {
        return flowTaskMapper.selectList(new LambdaQueryWrapper<>(FlowTask.class)
            .in(FlowTask::getInstanceId, instanceIdList));
    }

    /**
     * 按照实例id查询任务
     *
     * @param instanceId 流程实例id
     */
    @Override
    public List<FlowTask> selectByInstId(Long instanceId) {
        return flowTaskMapper.selectList(new LambdaQueryWrapper<>(FlowTask.class)
            .eq(FlowTask::getInstanceId, instanceId));
    }

    /**
     * 任务操作
     *
     * @param bo            参数
     * @param taskOperation 操作类型，委派 delegateTask、转办 transferTask、加签 addSignature、减签 reductionSignature
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean taskOperation(TaskOperationBo bo, String taskOperation) {
        FlowParams flowParams = new FlowParams();
        flowParams.message(bo.getMessage());
        if (LoginHelper.isSuperAdmin() || LoginHelper.isTenantAdmin()) {
            flowParams.ignore(true);
        }

        // 根据操作类型构建 FlowParams
        switch (taskOperation) {
            case DELEGATE_TASK, TRANSFER_TASK -> {
                ValidatorUtils.validate(bo, AddGroup.class);
                flowParams.addHandlers(Collections.singletonList(bo.getUserId()));
            }
            case ADD_SIGNATURE -> {
                ValidatorUtils.validate(bo, EditGroup.class);
                flowParams.addHandlers(bo.getUserIds());
            }
            case REDUCTION_SIGNATURE -> {
                ValidatorUtils.validate(bo, EditGroup.class);
                flowParams.reductionHandlers(bo.getUserIds());
            }
            default -> {
                log.error("Invalid operation type:{} ", taskOperation);
                throw new ServiceException("Invalid operation type " + taskOperation);
            }
        }

        Long taskId = bo.getTaskId();
        FlowTaskVo flowTaskVo = selectById(taskId);
        if ("addSignature".equals(taskOperation) || "reductionSignature".equals(taskOperation)) {
            if (flowTaskVo.getNodeRatio().compareTo(BigDecimal.ZERO) == 0) {
                throw new ServiceException(flowTaskVo.getNodeName() + "不是会签节点！");
            }
        }
        // 设置任务状态并执行对应的任务操作
        switch (taskOperation) {
            //委派任务
            case DELEGATE_TASK -> {
                flowParams.hisStatus(TaskStatusEnum.DEPUTE.getStatus());
                return taskService.depute(taskId, flowParams);
            }
            //转办任务
            case TRANSFER_TASK -> {
                flowParams.hisStatus(TaskStatusEnum.TRANSFER.getStatus());
                return taskService.transfer(taskId, flowParams);
            }
            //加签，增加办理人
            case ADD_SIGNATURE -> {
                flowParams.hisStatus(TaskStatusEnum.SIGN.getStatus());
                return taskService.addSignature(taskId, flowParams);
            }
            //减签，减少办理人
            case REDUCTION_SIGNATURE -> {
                flowParams.hisStatus(TaskStatusEnum.SIGN_OFF.getStatus());
                return taskService.reductionSignature(taskId, flowParams);
            }
            default -> {
                log.error("Invalid operation type:{} ", taskOperation);
                throw new ServiceException("Invalid operation type " + taskOperation);
            }
        }
    }

    /**
     * 修改任务办理人（此方法将会批量修改所有任务的办理人）
     *
     * @param taskIdList 任务id
     * @param userId     用户id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateAssignee(List<Long> taskIdList, String userId) {
        if (CollUtil.isEmpty(taskIdList)) {
            return false;
        }
        try {
            List<FlowTask> flowTasks = selectByIdList(taskIdList);
            // 批量删除现有任务的办理人记录
            if (CollUtil.isNotEmpty(flowTasks)) {
                WorkflowUtils.getFlowUserService().deleteByTaskIds(StreamUtils.toList(flowTasks, FlowTask::getId));
                List<User> userList = flowTasks.stream()
                    .map(flowTask -> {
                        FlowUser flowUser = new FlowUser();
                        flowUser.setType(UserType.APPROVAL.getKey());
                        flowUser.setProcessedBy(userId);
                        flowUser.setAssociated(flowTask.getId());
                        return flowUser;
                    })
                    .collect(Collectors.toList());
                if (CollUtil.isNotEmpty(userList)) {
                    WorkflowUtils.getFlowUserService().saveBatch(userList);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ServiceException(e.getMessage());
        }
        return true;
    }

    /**
     * 获取任务所有办理人
     *
     * @param taskIdList 任务id
     */
    @Override
    public Map<Long, List<UserDTO>> currentTaskAllUser(List<Long> taskIdList) {
        Map<Long, List<UserDTO>> map = new HashMap<>();
        // 获取与当前任务关联的用户列表
        List<User> associatedUsers = WorkflowUtils.getFlowUserService().getByAssociateds(taskIdList);
        Map<Long, List<User>> listMap = StreamUtils.groupByKey(associatedUsers, User::getAssociated);
        for (Map.Entry<Long, List<User>> entry : listMap.entrySet()) {
            List<User> value = entry.getValue();
            if (CollUtil.isNotEmpty(value)) {
                List<UserDTO> userDTOS = userService.selectListByIds(StreamUtils.toList(value, e -> Long.valueOf(e.getProcessedBy())));
                map.put(entry.getKey(), userDTOS);
            }
        }
        return map;
    }

    /**
     * 获取当前任务的所有办理人
     *
     * @param taskId 任务id
     */
    @Override
    public List<UserDTO> currentTaskAllUser(Long taskId) {
        // 获取与当前任务关联的用户列表
        List<User> userList = WorkflowUtils.getFlowUserService().getByAssociateds(Collections.singletonList(taskId));
        if (CollUtil.isEmpty(userList)) {
            return Collections.emptyList();
        }
        return userService.selectListByIds(StreamUtils.toList(userList, e -> Long.valueOf(e.getProcessedBy())));
    }
}
