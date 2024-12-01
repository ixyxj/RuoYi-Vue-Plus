package org.dromara.workflow.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.warm.flow.core.entity.HisTask;
import org.dromara.workflow.domain.bo.*;
import org.dromara.workflow.domain.vo.FlowHisTaskVo;
import org.dromara.workflow.domain.vo.FlowTaskVo;

import java.util.List;
import java.util.Map;

/**
 * 任务 服务层
 *
 * @author may
 */
public interface IFlwTaskService {
    /**
     * 启动任务
     *
     * @param startProcessBo 启动流程参数
     * @return 结果
     */
    Map<String, Object> startWorkFlow(StartProcessBo startProcessBo);


    /**
     * 办理任务
     *
     * @param completeTaskBo 办理任务参数
     * @return 结果
     */
    boolean completeTask(CompleteTaskBo completeTaskBo);

    /**
     * 查询当前用户的待办任务
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     * @return 结果
     */
    TableDataInfo<FlowTaskVo> getPageByTaskWait(FlowTaskBo flowTaskBo, PageQuery pageQuery);

    /**
     * 查询当前租户所有待办任务
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     * @return 结果
     */
    TableDataInfo<FlowHisTaskVo> getPageByTaskFinish(FlowTaskBo flowTaskBo, PageQuery pageQuery);


    /**
     * 查询待办任务
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     * @return 结果
     */
    TableDataInfo<FlowTaskVo> getPageByAllTaskWait(FlowTaskBo flowTaskBo, PageQuery pageQuery);

    /**
     * 查询已办任务
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     * @return 结果
     */
    TableDataInfo<FlowHisTaskVo> getPageByAllTaskFinish(FlowTaskBo flowTaskBo, PageQuery pageQuery);

    /**
     * 查询当前用户的抄送
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     * @return 结果
     */
    TableDataInfo<FlowTaskVo> getPageByTaskCopy(FlowTaskBo flowTaskBo, PageQuery pageQuery);

    /**
     * 驳回审批
     *
     * @param bo 参数
     * @return 结果
     */
    boolean backProcess(BackProcessBo bo);

    /**
     * 获取可驳回节点
     *
     * @param instanceId 实例id
     * @return 结果
     */
    List<HisTask> getBackTaskNode(String instanceId);

    /**
     * 终止任务
     *
     * @param bo 参数
     * @return 结果
     */
    boolean terminationTask(FlowTerminationBo bo);
}
