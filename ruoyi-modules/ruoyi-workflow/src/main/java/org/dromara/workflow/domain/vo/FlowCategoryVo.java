package org.dromara.workflow.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.common.excel.annotation.ExcelDictFormat;
import org.dromara.common.excel.convert.ExcelDictConvert;
import org.dromara.workflow.domain.FlowCategory;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;


/**
 * 流程分类视图对象 wf_category
 *
 * @author may
 * @date 2023-06-27
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = FlowCategory.class)
public class FlowCategoryVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 流程分类ID
     */
    @ExcelProperty(value = "流程分类ID")
    private Long categoryId;

    /**
     * 父部门id
     */
    private Long parentId;

    /**
     * 父部门名称
     */
    private String parentName;

    /**
     * 祖级列表
     */
    private String ancestors;

    /**
     * 流程分类名称
     */
    @ExcelProperty(value = "流程分类名称")
    private String categoryName;

    /**
     * 流程分类编码
     */
    @ExcelProperty(value = "流程分类编码")
    private String categoryCode;

    /**
     * 显示顺序
     */
    @ExcelProperty(value = "显示顺序")
    private Long orderNum;

    /**
     * 流程分类状态（0正常 1停用）
     */
    @ExcelProperty(value = "流程分类状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "0=正常,1=停用")
    private String status;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;

}
