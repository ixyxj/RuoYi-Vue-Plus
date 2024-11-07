package org.dromara.common.core.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 任务受让人
 *
 * @author AprilWind
 */
@Data
@NoArgsConstructor
public class TaskAssigneeDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 总大小
     */
    private Long total = 0L;

    /**
     *
     */
    private List<TaskHandler> list;

    public TaskAssigneeDTO(Long total, List<TaskHandler> list) {
        this.total = total;
        this.list = list;
    }

    /**
     * 将源列表转换为 TaskHandler 列表
     *
     * @param <T>              通用类型
     * @param sourceList       待转换的源列表
     * @param storageId        提取 storageId 的函数
     * @param handlerCode      提取 handlerCode 的函数
     * @param handlerName      提取 handlerName 的函数
     * @param groupName        提取 groupName 的函数
     * @param createTimeMapper 提取 createTime 的函数
     * @return 转换后的 TaskHandler 列表
     */
    public static <T> List<TaskHandler> convertToHandlerList(
        List<T> sourceList,
        Function<T, Long> storageId,
        Function<T, String> handlerCode,
        Function<T, String> handlerName,
        Function<T, String> groupName,
        Function<T, Date> createTimeMapper) {
        return sourceList.stream()
            .map(item -> new TaskHandler(
                String.valueOf(storageId.apply(item)),
                handlerCode != null ? handlerCode.apply(item) : "无",
                handlerName != null ? handlerName.apply(item) : "无",
                groupName != null ? groupName.apply(item) : "默认分组",
                createTimeMapper != null ? createTimeMapper.apply(item) : new Date()
            ))
            .collect(Collectors.toList());
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskHandler {

        /**
         * 主键
         */
        private String storageId;

        /**
         * 权限编码
         */
        private String handlerCode = "无";

        /**
         * 权限名称
         */
        private String handlerName = "无";

        /**
         * 权限分组
         */
        private String groupName = "默认分组";

        /**
         * 创建时间
         */
        private Date createTime;
    }

}
