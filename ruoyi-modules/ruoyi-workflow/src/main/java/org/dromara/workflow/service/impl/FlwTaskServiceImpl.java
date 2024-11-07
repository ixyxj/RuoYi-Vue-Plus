package org.dromara.workflow.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warm.flow.core.dto.FlowParams;
import com.warm.flow.core.entity.*;
import com.warm.flow.core.enums.FlowStatus;
import com.warm.flow.core.enums.NodeType;
import com.warm.flow.core.enums.SkipType;
import com.warm.flow.core.service.DefService;
import com.warm.flow.core.service.InsService;
import com.warm.flow.core.service.TaskService;
import com.warm.flow.core.service.UserService;
import com.warm.flow.orm.entity.FlowHisTask;
import com.warm.flow.orm.entity.FlowInstance;
import com.warm.flow.orm.entity.FlowSkip;
import com.warm.flow.orm.entity.FlowTask;
import com.warm.flow.orm.mapper.FlowHisTaskMapper;
import com.warm.flow.orm.mapper.FlowSkipMapper;
import com.warm.flow.orm.mapper.FlowTaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.enums.BusinessStatusEnum;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.workflow.common.enums.TaskStatusEnum;
import org.dromara.workflow.domain.bo.*;
import org.dromara.workflow.domain.vo.FlowHisTaskVo;
import org.dromara.workflow.domain.vo.FlowTaskVo;
import org.dromara.workflow.domain.vo.WfDefinitionConfigVo;
import org.dromara.workflow.handler.FlowProcessEventHandler;
import org.dromara.workflow.mapper.FlwTaskMapper;
import org.dromara.workflow.service.IFlwInstanceService;
import org.dromara.workflow.service.IFlwTaskService;
import org.dromara.workflow.service.IWfDefinitionConfigService;
import org.dromara.workflow.utils.WorkflowUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static org.dromara.workflow.common.constant.FlowConstant.BUSINESS_KEY;
import static org.dromara.workflow.common.constant.FlowConstant.INITIATOR;

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
    private final FlwTaskMapper flwTaskMapper;
    private final UserService userService;
    private final IWfDefinitionConfigService wfDefinitionConfigService;
    private final IFlwInstanceService iFlwInstanceService;
    private final FlowTaskMapper flowTaskMapper;
    private final FlowHisTaskMapper flowHisTaskMapper;
    private final FlowSkipMapper flowSkipMapper;
    private final FlowProcessEventHandler flowProcessEventHandler;
    private final DefService defService;

    /**
     * 启动任务
     *
     * @param startProcessBo 启动流程参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> startWorkFlow(StartProcessBo startProcessBo) {
        String businessKey = startProcessBo.getBusinessKey();
        String userId = LoginHelper.getUserIdStr();
        if (StringUtils.isBlank(businessKey)) {
            throw new ServiceException("启动工作流时必须包含业务ID");
        }
        // 启动流程实例（提交申请）
        Map<String, Object> variables = startProcessBo.getVariables();
        // 流程发起人
        variables.put(INITIATOR, userId);
        // 业务id
        variables.put(BUSINESS_KEY, businessKey);
        WfDefinitionConfigVo wfDefinitionConfigVo = wfDefinitionConfigService.getByTableNameLastVersion(startProcessBo.getTableName());
        if (wfDefinitionConfigVo == null) {
            throw new ServiceException("请到流程定义绑定业务表名与流程KEY！");
        }

        FlowInstance flowInstance = iFlwInstanceService.instanceByBusinessId(businessKey);
        if (flowInstance != null) {
            List<Task> taskList = taskService.list(new FlowTask().setInstanceId(flowInstance.getId()));
            return Map.of("processInstanceId", taskList.get(0).getInstanceId(), "taskId", taskList.get(0).getId());
        }
        FlowParams flowParams = new FlowParams();
        flowParams.flowCode(wfDefinitionConfigVo.getProcessKey());
        flowParams.variable(startProcessBo.getVariables());
        flowParams.setHandler(userId);
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
            // 获取当前用户ID作为任务处理人
            String userId = LoginHelper.getUserIdStr();

            // 获取任务ID并查询对应的流程任务和实例信息
            Long taskId = completeTaskBo.getTaskId();
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
            flowParams.handler(userId);
            flowParams.permissionFlag(WorkflowUtils.permissionList());
            flowParams.flowStatus(BusinessStatusEnum.WAITING.getStatus()).hisStatus(TaskStatusEnum.PASS.getStatus());

            // 执行任务跳转，并根据返回的处理人设置下一步处理人
            setHandler(taskService.skip(taskId, flowParams));

            // 更新实例状态为待审核状态
            iFlwInstanceService.updateStatus(ins.getId(), BusinessStatusEnum.WAITING.getStatus());
            //判断是否流程结束
            Instance instance = insService.getById(ins.getId());
            // 重新获取实例信息，检查流程是否已结束
            if (FlowStatus.isFinished(instance.getFlowStatus())) {
                // 若流程已结束，更新状态为已完成
                iFlwInstanceService.updateStatus(instance.getId(), BusinessStatusEnum.FINISH.getStatus());
                // 流程结束监听，处理结束后的业务逻辑
                flowProcessEventHandler.processHandler(definition.getFlowCode(), instance.getBusinessId(),
                    BusinessStatusEnum.FINISH.getStatus(), false);
            }
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * 设置办理人
     *
     * @param instance 实例
     */
    private void setHandler(Instance instance) {
        if (instance != null) {
            // 根据流程实例ID查询所有关联的任务
            List<FlowTask> flowTasks = flowTaskMapper.selectList(new LambdaQueryWrapper<>(FlowTask.class)
                .eq(FlowTask::getInstanceId, instance.getId()));

            // 遍历任务列表，处理每个任务的办理人
            for (FlowTask flowTask : flowTasks) {
                // 获取与当前任务关联的用户列表
                List<User> userList = userService.getByAssociateds(Collections.singletonList(flowTask.getId()));
                // 通过工具方法过滤和获取有效用户
                Set<User> users = WorkflowUtils.getUser(userList);
                if (CollUtil.isNotEmpty(users)) {
                    // 删除现有的任务办理人记录，确保后续数据清理和更新
                    userService.deleteByTaskIds(Collections.singletonList(flowTask.getId()));
                    // 将新的办理人关联到任务ID，并批量保存新的办理人列表
                    users.forEach(user -> user.setAssociated(flowTask.getId()));
                    userService.saveBatch(new ArrayList<>(users));
                }
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
        QueryWrapper<FlowTaskBo> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("t.processed_by", WorkflowUtils.permissionList());
        queryWrapper.in("t.flow_status", BusinessStatusEnum.WAITING.getStatus());
        Page<FlowTaskVo> page = buildTaskWaitingPage(pageQuery, queryWrapper, flowTaskBo);
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
        QueryWrapper<FlowTaskBo> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("t.approver", LoginHelper.getUserId());
        Page<FlowHisTaskVo> page = buildTaskFinishPage(pageQuery, queryWrapper, flowTaskBo);
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
        QueryWrapper<FlowTaskBo> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("t.processed_by", WorkflowUtils.permissionList());
        Page<FlowTaskVo> page = buildTaskWaitingPage(pageQuery, queryWrapper, flowTaskBo);
        return TableDataInfo.build(page);
    }

    /**
     * 构建待处理任务分页对象
     *
     * @param pageQuery    分页查询对象
     * @param queryWrapper 查询条件封装对象
     * @param flowTaskBo   流程任务业务对象
     * @return 分页后的待处理任务列表
     */
    private Page<FlowTaskVo> buildTaskWaitingPage(PageQuery pageQuery, QueryWrapper<FlowTaskBo> queryWrapper, FlowTaskBo flowTaskBo) {
        commonCondition(queryWrapper, flowTaskBo);
        Page<FlowTaskVo> page = flwTaskMapper.getTaskWaitByPage(pageQuery.build(), queryWrapper);
        List<FlowTaskVo> records = page.getRecords();
        if (CollUtil.isNotEmpty(records)) {
            List<Long> taskIds = StreamUtils.toList(records, FlowTaskVo::getId);
            List<User> userList = userService.getByAssociateds(taskIds);
            for (FlowTaskVo data : records) {
                if (CollUtil.isNotEmpty(userList)) {
                    List<User> users = StreamUtils.filter(userList, e -> e.getAssociated().toString().equals(data.getId().toString()));
                    data.setUserList(CollUtil.isEmpty(users) ? Collections.emptyList() : users);
                    data.setUserDTOList(WorkflowUtils.getHandlerUser(users));
                }
                data.setFlowStatusName(FlowStatus.getValueByKey(data.getFlowStatus()));
            }
        }
        return page;
    }

    /**
     * 通用条件
     *
     * @param queryWrapper 查询条件
     * @param flowTaskBo   参数
     */
    private void commonCondition(QueryWrapper<FlowTaskBo> queryWrapper, FlowTaskBo flowTaskBo) {
        queryWrapper.like(StringUtils.isNotBlank(flowTaskBo.getNodeName()), "t.node_name", flowTaskBo.getNodeName());
        queryWrapper.like(StringUtils.isNotBlank(flowTaskBo.getFlowName()), "t.flow_name", flowTaskBo.getFlowName());
        queryWrapper.eq(StringUtils.isNotBlank(flowTaskBo.getFlowCode()), "t.flow_code", flowTaskBo.getFlowCode());
        queryWrapper.eq("t.node_type", NodeType.BETWEEN.getKey());
        queryWrapper.orderByDesc("t.create_time");
    }

    /**
     * 查询已办任务
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     */
    @Override
    public TableDataInfo<FlowHisTaskVo> getPageByAllTaskFinish(FlowTaskBo flowTaskBo, PageQuery pageQuery) {
        QueryWrapper<FlowTaskBo> queryWrapper = new QueryWrapper<>();
        Page<FlowHisTaskVo> page = buildTaskFinishPage(pageQuery, queryWrapper, flowTaskBo);
        return TableDataInfo.build(page);
    }

    /**
     * 构建已完成任务分页对象
     *
     * @param pageQuery    分页查询对象
     * @param queryWrapper 查询条件封装对象
     * @param flowTaskBo   流程任务业务对象
     * @return 分页后的已完成任务列表
     */
    private Page<FlowHisTaskVo> buildTaskFinishPage(PageQuery pageQuery, QueryWrapper<FlowTaskBo> queryWrapper, FlowTaskBo flowTaskBo) {
        commonCondition(queryWrapper, flowTaskBo);
        Page<FlowHisTaskVo> page = flwTaskMapper.getTaskFinishByPage(pageQuery.build(), queryWrapper);
        List<FlowHisTaskVo> records = page.getRecords();
        // 如果有任务记录，为每条记录设置流程状态名称
        if (CollUtil.isNotEmpty(records)) {
            records.forEach(data -> data.setFlowStatusName(FlowStatus.getValueByKey(data.getFlowStatus())));
        }
        return page;
    }

    /**
     * 查询当前用户的抄送
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     */
    @Override
    public TableDataInfo<FlowTaskVo> getPageByTaskCopy(FlowTaskBo flowTaskBo, PageQuery pageQuery) {
        QueryWrapper<FlowTaskBo> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StringUtils.isNotBlank(flowTaskBo.getFlowName()), "t.flow_name", flowTaskBo.getFlowName());
        queryWrapper.eq(StringUtils.isNotBlank(flowTaskBo.getFlowCode()), "t.flow_code", flowTaskBo.getFlowCode());
        Page<FlowTaskVo> page = flwTaskMapper.getTaskCopyByPage(pageQuery.build(), queryWrapper);
        return TableDataInfo.build(page);
    }

    /**
     * 驳回任务
     *
     * @param bo 参数
     */
    @Override
    public boolean backProcess(BackProcessBo bo) {
        try {
            String userId = LoginHelper.getUserIdStr();
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
            if (nextNodeCode.equals(bo.getNodeCode())) {
                flowParams.skipType(SkipType.REJECT.getKey());
                flowParams.flowStatus(BusinessStatusEnum.BACK.getStatus());
            } else {
                flowParams.skipType(SkipType.PASS.getKey());
                flowParams.flowStatus(BusinessStatusEnum.WAITING.getStatus());
            }
            flowParams.hisStatus(TaskStatusEnum.BACK.getStatus());
            flowParams.message(bo.getMessage());
            flowParams.handler(userId);
            flowParams.nodeCode(bo.getNodeCode());
            flowParams.setPermissionFlag(WorkflowUtils.permissionList());
            Instance instance = taskService.skip(taskId, flowParams);
            setHandler(instance);
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
            .eq(FlowHisTask::getNodeType, 1)
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
    public boolean terminationTask(TerminationBo bo) {
        try {
            FlowTask flowTask = flowTaskMapper.selectById(bo.getTaskId());
            Instance ins = insService.getById(flowTask.getInstanceId());
            //流程定义
            Definition definition = defService.getById(flowTask.getDefinitionId());
            FlowParams flowParams = new FlowParams();
            flowParams.handler(LoginHelper.getUserIdStr());
            flowParams.message(bo.getComment());
            flowParams.permissionFlag(WorkflowUtils.permissionList());
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
}
