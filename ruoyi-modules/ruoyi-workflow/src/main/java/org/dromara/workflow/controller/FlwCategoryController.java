package org.dromara.workflow.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.excel.utils.ExcelUtil;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.dromara.workflow.domain.bo.FlowCategoryBo;
import org.dromara.workflow.domain.vo.FlowCategoryVo;
import org.dromara.workflow.service.IFlwCategoryService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 流程分类
 *
 * @author may
 * @date 2023-06-28
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/workflow/category")
public class FlwCategoryController extends BaseController {

    private final IFlwCategoryService FlowCategoryService;

    /**
     * 查询流程分类列表
     */
    @GetMapping("/list")
    public R<List<FlowCategoryVo>> list(FlowCategoryBo bo) {
        List<FlowCategoryVo> list = FlowCategoryService.queryList(bo);
        return R.ok(list);

    }

    /**
     * 导出流程分类列表
     */
    @SaCheckPermission("workflow:category:export")
    @Log(title = "流程分类", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(FlowCategoryBo bo, HttpServletResponse response) {
        List<FlowCategoryVo> list = FlowCategoryService.queryList(bo);
        ExcelUtil.exportExcel(list, "流程分类", FlowCategoryVo.class, response);
    }

    /**
     * 获取流程分类详细信息
     *
     * @param id 主键
     */
    @GetMapping("/{id}")
    public R<FlowCategoryVo> getInfo(@NotNull(message = "主键不能为空")
                                   @PathVariable Long id) {
        return R.ok(FlowCategoryService.queryById(id));
    }

    /**
     * 新增流程分类
     */
    @SaCheckPermission("workflow:category:add")
    @Log(title = "流程分类", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody FlowCategoryBo bo) {
        return toAjax(FlowCategoryService.insertByBo(bo));
    }

    /**
     * 修改流程分类
     */
    @SaCheckPermission("workflow:category:edit")
    @Log(title = "流程分类", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody FlowCategoryBo bo) {
        return toAjax(FlowCategoryService.updateByBo(bo));
    }

    /**
     * 删除流程分类
     *
     * @param ids 主键串
     */
    @SaCheckPermission("workflow:category:remove")
    @Log(title = "流程分类", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(FlowCategoryService.deleteWithValidByIds(List.of(ids), true));
    }
}
