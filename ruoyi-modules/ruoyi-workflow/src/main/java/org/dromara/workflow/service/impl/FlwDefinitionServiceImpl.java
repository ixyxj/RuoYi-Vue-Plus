package org.dromara.workflow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.warm.flow.core.service.DefService;
import org.dromara.warm.flow.orm.entity.FlowDefinition;
import org.dromara.warm.flow.orm.entity.FlowHisTask;
import org.dromara.warm.flow.orm.mapper.FlowDefinitionMapper;
import org.dromara.warm.flow.orm.mapper.FlowHisTaskMapper;
import org.dromara.workflow.domain.vo.FlowDefinitionVo;
import org.dromara.workflow.mapper.FlwDefMapper;
import org.dromara.workflow.service.IFlwDefinitionService;
import org.dromara.workflow.service.IWfDefinitionConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 流程定义 服务层实现
 *
 * @author may
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class FlwDefinitionServiceImpl implements IFlwDefinitionService {
    private final DefService defService;
    private final FlowDefinitionMapper flowDefinitionMapper;
    private final FlwDefMapper flwDefMapper;
    private final FlowHisTaskMapper flowHisTaskMapper;
    private final IWfDefinitionConfigService wfDefinitionConfigService;

    /**
     * 分页查询
     *
     * @param flowDefinition 参数
     * @return 返回分页列表
     */
    @Override
    public TableDataInfo<FlowDefinitionVo> page(FlowDefinition flowDefinition, PageQuery pageQuery) {
        QueryWrapper<FlowDefinition> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("create_time");
        Page<FlowDefinition> page = flwDefMapper.page(pageQuery.build(), queryWrapper);
        TableDataInfo<FlowDefinitionVo> build = TableDataInfo.build();
        build.setRows(BeanUtil.copyToList(page.getRecords(), FlowDefinitionVo.class));
        build.setTotal(page.getTotal());
        return build;
    }

    /**
     * 获取历史流程定义列表
     *
     * @param flowCode 参数
     */
    @Override
    public List<FlowDefinitionVo> getHisListByKey(String flowCode) {
        LambdaQueryWrapper<FlowDefinition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FlowDefinition::getFlowCode, flowCode);
        List<FlowDefinition> list = flowDefinitionMapper.selectList(wrapper);
        return BeanUtil.copyToList(list, FlowDefinitionVo.class);
    }

    /**
     * 导入流程定义
     *
     * @param file 文件
     */
    @Override
    public boolean importXml(MultipartFile file) {
        try {
            defService.importXml(file.getInputStream());
        } catch (Exception e) {
            log.error("导入流程定义错误: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
        return true;
    }

    /**
     * 导出流程定义
     *
     * @param id       流程定义id
     * @param response 响应
     * @throws IOException 异常
     */
    @Override
    public void exportDef(Long id, HttpServletResponse response) throws IOException {
        Document document = defService.exportXml(id);
        // 设置生成xml的格式
        OutputFormat of = OutputFormat.createPrettyPrint();
        // 设置编码格式
        of.setEncoding("UTF-8");
        of.setIndent(true);
        of.setIndent("    ");
        of.setNewlines(true);

        // 创建一个xml文档编辑器
        XMLWriter writer = new XMLWriter(response.getOutputStream(), of);
        writer.setEscapeText(false);
        response.reset();
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/x-msdownload");
        response.setHeader("Content-Disposition", "attachment;");
        writer.write(document);
        writer.close();
    }

    /**
     * 删除流程定义
     *
     * @param ids 流程定义id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeDef(List<Long> ids) {
        LambdaQueryWrapper<FlowHisTask> wrapper = Wrappers.lambdaQuery();
        wrapper.in(FlowHisTask::getDefinitionId, ids);
        List<FlowHisTask> flowHisTasks = flowHisTaskMapper.selectList(wrapper);
        if (CollUtil.isNotEmpty(flowHisTasks)) {
            List<FlowDefinition> flowDefinitions = flowDefinitionMapper.selectByIds(StreamUtils.toList(flowHisTasks, FlowHisTask::getDefinitionId));
            if (CollUtil.isNotEmpty(flowDefinitions)) {
                String join = StreamUtils.join(flowDefinitions, FlowDefinition::getFlowCode);
                log.error("流程定义【{}】已被使用不可被删除！", join);
                throw new ServiceException("流程定义【" + join + "】已被使用不可被删除！");
            }
        }
        try {
            defService.removeDef(ids);
            wfDefinitionConfigService.deleteByDefIds(ids);
        } catch (Exception e) {
            log.error("Error removing flow definitions: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to remove flow definitions", e);
        }
        return true;
    }
}
