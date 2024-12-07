package org.dromara.workflow.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.enums.BusinessStatusEnum;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.warm.flow.core.entity.HisTask;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.core.entity.Task;
import org.dromara.warm.flow.core.service.InsService;
import org.dromara.warm.flow.core.service.TaskService;
import org.dromara.workflow.domain.bo.*;
import org.dromara.workflow.domain.vo.FlowHisTaskVo;
import org.dromara.workflow.domain.vo.FlowTaskVo;
import org.dromara.workflow.service.IFlwTaskService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 任务管理 控制层
 *
 * @author may
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/workflow/task")
public class FlwTaskController extends BaseController {

    private final IFlwTaskService flwTaskService;
    private final TaskService taskService;
    private final InsService insService;

    /**
     * 启动任务
     *
     * @param startProcessBo 启动流程参数
     */
    @Log(title = "任务管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/startWorkFlow")
    public R<Map<String, Object>> startWorkFlow(@Validated(AddGroup.class) @RequestBody StartProcessBo startProcessBo) {
        Map<String, Object> map = flwTaskService.startWorkFlow(startProcessBo);
        return R.ok("提交成功", map);
    }

    /**
     * 办理任务
     *
     * @param completeTaskBo 办理任务参数
     */
    @Log(title = "任务管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/completeTask")
    public R<Void> completeTask(@Validated(AddGroup.class) @RequestBody CompleteTaskBo completeTaskBo) {
        return toAjax(flwTaskService.completeTask(completeTaskBo));
    }

    /**
     * 查询当前用户的待办任务
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     */
    @GetMapping("/getPageByTaskWait")
    public TableDataInfo<FlowTaskVo> getPageByTaskWait(FlowTaskBo flowTaskBo, PageQuery pageQuery) {
        return flwTaskService.getPageByTaskWait(flowTaskBo, pageQuery);
    }

    /**
     * 查询当前用户的已办任务
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     */

    @GetMapping("/getPageByTaskFinish")
    public TableDataInfo<FlowHisTaskVo> getPageByTaskFinish(FlowTaskBo flowTaskBo, PageQuery pageQuery) {
        return flwTaskService.getPageByTaskFinish(flowTaskBo, pageQuery);
    }

    /**
     * 查询待办任务
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     */
    @GetMapping("/getPageByAllTaskWait")
    public TableDataInfo<FlowTaskVo> getPageByAllTaskWait(FlowTaskBo flowTaskBo, PageQuery pageQuery) {
        return flwTaskService.getPageByAllTaskWait(flowTaskBo, pageQuery);
    }

    /**
     * 查询已办任务
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     */

    @GetMapping("/getPageByAllTaskFinish")
    public TableDataInfo<FlowHisTaskVo> getPageByAllTaskFinish(FlowTaskBo flowTaskBo, PageQuery pageQuery) {
        return flwTaskService.getPageByAllTaskFinish(flowTaskBo, pageQuery);
    }

    /**
     * 查询当前用户的抄送
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     */
    @GetMapping("/getPageByTaskCopy")
    public TableDataInfo<FlowTaskVo> getPageByTaskCopy(FlowTaskBo flowTaskBo, PageQuery pageQuery) {
        return flwTaskService.getPageByTaskCopy(flowTaskBo, pageQuery);
    }

    /**
     * 根据taskId查询代表任务
     *
     * @param taskId 任务id
     */
    @GetMapping("/getTaskById/{taskId}")
    public R<FlowTaskVo> getTaskById(@PathVariable Long taskId) {
        return R.ok(flwTaskService.selectById(taskId));
    }

    /**
     * 终止任务
     *
     * @param bo 参数
     */
    @Log(title = "任务管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/terminationTask")
    public R<Boolean> terminationTask(@RequestBody FlowTerminationBo bo) {
        return R.ok(flwTaskService.terminationTask(bo));
    }

    /**
     * 任务操作
     *
     * @param bo            参数
     * @param taskOperation 操作类型，委派 delegateTask、转办 transferTask、加签 addSignature、减签 reductionSignature
     */
    @Log(title = "任务管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/taskOperation/{taskOperation}")
    public R<Void> taskOperation(@Validated @RequestBody TaskOperationBo bo, @PathVariable String taskOperation) {
        return toAjax(flwTaskService.taskOperation(bo, taskOperation));
    }

    /**
     * 修改任务办理人
     *
     * @param taskIdList 任务id
     * @param userId     办理人id
     */
    @Log(title = "任务管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping("/updateAssignee/{userId}")
    public R<Void> updateAssignee(@RequestBody List<Long> taskIdList, @PathVariable String userId) {
        return toAjax(flwTaskService.updateAssignee(taskIdList, userId));
    }

    /**
     * 驳回审批
     *
     * @param bo 参数
     */
    @Log(title = "任务管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/backProcess")
    public R<Void> backProcess(@Validated({AddGroup.class}) @RequestBody BackProcessBo bo) {
        return toAjax(flwTaskService.backProcess(bo));
    }

    /**
     * 获取可驳回节点
     *
     * @param instanceId 实例id
     */
    @GetMapping("/getBackTaskNode/{instanceId}")
    public R<List<HisTask>> getBackTaskNode(@PathVariable String instanceId) {
        return R.ok(flwTaskService.getBackTaskNode(instanceId));
    }

}
