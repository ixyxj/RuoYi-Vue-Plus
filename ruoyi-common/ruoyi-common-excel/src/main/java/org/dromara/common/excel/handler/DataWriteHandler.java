package org.dromara.common.excel.handler;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.excel.metadata.data.DataFormatData;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.util.StyleUtil;
import com.alibaba.excel.write.handler.CellWriteHandler;
import com.alibaba.excel.write.handler.SheetWriteHandler;
import com.alibaba.excel.write.handler.context.CellWriteHandlerContext;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.metadata.style.WriteFont;
import lombok.Data;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;

import java.util.Map;

/**
 * 批注、必填
 *
 * @author guzhouyanyu
 */
@Data
public class DataWriteHandler implements SheetWriteHandler, CellWriteHandler {

    /**
     * 批注
     */
    private final Map<Integer, String> notationMap;

    /**
     * 头列字体颜色
     */
    private final Map<Integer, Short> headColumnMap;


    @Override
    public void afterCellDispose(CellWriteHandlerContext context) {
        WriteCellData<?> cellData = context.getFirstCellData();
        WriteCellStyle writeCellStyle = cellData.getOrCreateStyle();

        DataFormatData dataFormatData = new DataFormatData();
        // 单元格设置为文本格式
        dataFormatData.setIndex((short) 49);
        writeCellStyle.setDataFormatData(dataFormatData);

        if (context.getHead()) {
            Cell cell = context.getCell();
            WriteSheetHolder writeSheetHolder = context.getWriteSheetHolder();
            Sheet sheet = writeSheetHolder.getSheet();
            Workbook workbook = writeSheetHolder.getSheet().getWorkbook();
            Drawing<?> drawing = sheet.createDrawingPatriarch();
            // 设置标题字体样式
            WriteFont headWriteFont = new WriteFont();
            // 加粗
            headWriteFont.setBold(true);
            if (CollUtil.isNotEmpty(headColumnMap) && headColumnMap.containsKey(cell.getColumnIndex())) {
                // 设置字体颜色
                headWriteFont.setColor(headColumnMap.get(cell.getColumnIndex()));
            }
            writeCellStyle.setWriteFont(headWriteFont);
            CellStyle cellStyle = StyleUtil.buildCellStyle(workbook, null, writeCellStyle);
            cell.setCellStyle(cellStyle);

            if (CollUtil.isNotEmpty(notationMap) && notationMap.containsKey(cell.getColumnIndex())) {
                // 批注内容
                String notationContext = notationMap.get(cell.getColumnIndex());
                // 创建绘图对象
                Comment comment = drawing.createCellComment(new XSSFClientAnchor(0, 0, 0, 0, (short) cell.getColumnIndex(), 0, (short) 5, 5));
                comment.setString(new XSSFRichTextString(notationContext));
                cell.setCellComment(comment);
            }
        }
    }
}
