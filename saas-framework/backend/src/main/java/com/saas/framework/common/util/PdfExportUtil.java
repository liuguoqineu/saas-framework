package com.saas.framework.common.util;

import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.saas.framework.entity.report.RpReport;
import com.saas.framework.entity.report.RpTemplate;
import com.saas.framework.entity.SysUser;
import lombok.extern.slf4j.Slf4j;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
public class PdfExportUtil {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm");

    public static void exportReportPdf(RpReport report, RpTemplate template, SysUser user, OutputStream outputStream) throws DocumentException, IOException {
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        PdfWriter.getInstance(document, outputStream);

        document.open();

        BaseFont chineseFont = getChineseFont();
        Font titleFont = new Font(chineseFont, 18, Font.BOLD);
        Font headerFont = new Font(chineseFont, 12, Font.BOLD);
        Font normalFont = new Font(chineseFont, 10, Font.NORMAL);
        Font labelFont = new Font(chineseFont, 10, Font.BOLD);

        addHeader(document, report, template, user, titleFont, headerFont, normalFont, labelFont);
        addContent(document, report, normalFont, labelFont, chineseFont);
        addFooter(document, report, normalFont);

        document.close();
    }

    private static void addHeader(Document document, RpReport report, RpTemplate template, SysUser user,
                                  Font titleFont, Font headerFont, Font normalFont, Font labelFont)
            throws DocumentException {

        String reportTypeName = getReportTypeName(report.getReportType());
        Paragraph title = new Paragraph(reportTypeName, titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        PdfPTable infoTable = new PdfPTable(4);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[]{1.5f, 2.5f, 1.5f, 2.5f});
        infoTable.setSpacingBefore(10);
        infoTable.setSpacingAfter(15);

        addInfoRow(infoTable, "填报人：", user != null ? user.getRealName() : "未知", labelFont, normalFont);
        addInfoRow(infoTable, "岗位类型：", getPostTypeName(user), labelFont, normalFont);
        addInfoRow(infoTable, "填报周期：", formatPeriod(report.getReportPeriod(), report.getReportType()), labelFont, normalFont);
        addInfoRow(infoTable, "报表状态：", getStatusName(report.getStatus()), labelFont, normalFont);
        if (report.getSubmitTime() != null) {
            addInfoRow(infoTable, "提交时间：", report.getSubmitTime().format(DATE_FORMATTER), labelFont, normalFont);
        }

        document.add(infoTable);

        Paragraph separator = new Paragraph(" ", normalFont);
        separator.setSpacingBefore(5);
        separator.setSpacingAfter(10);
        document.add(separator);
    }

    private static void addContent(Document document, RpReport report,
                                   Font normalFont, Font labelFont, BaseFont chineseFont)
            throws DocumentException {

        Paragraph contentTitle = new Paragraph("报表内容", labelFont);
        contentTitle.setSpacingBefore(15);
        contentTitle.setSpacingAfter(10);
        document.add(contentTitle);

        if (report.getContentText() != null && !report.getContentText().trim().isEmpty()) {
            String[] lines = report.getContentText().split("\n");
            for (String line : lines) {
                if (line.trim().startsWith("【") && line.trim().endsWith("】")) {
                    Paragraph sectionTitle = new Paragraph(line.trim(), labelFont);
                    sectionTitle.setSpacingBefore(8);
                    sectionTitle.setSpacingAfter(5);
                    document.add(sectionTitle);
                } else if (line.trim().startsWith("- ") || line.trim().startsWith("• ")) {
                    ListItem item = new ListItem(line.trim().replaceFirst("^[-•]\\s*", "• "), normalFont);
                    item.setIndentationLeft(15);
                    document.add(item);
                } else if (!line.trim().isEmpty()) {
                    Paragraph para = new Paragraph(line.trim(), normalFont);
                    para.setIndentationLeft(10);
                    para.setSpacingAfter(3);
                    document.add(para);
                }
            }
        } else {
            Paragraph empty = new Paragraph("（暂无内容）", new Font(chineseFont, 10, Font.ITALIC, Color.GRAY));
            document.add(empty);
        }
    }

    private static void addFooter(Document document, RpReport report, Font normalFont) throws DocumentException {
        document.add(new Paragraph(" "));
        
        Paragraph separator = new Paragraph(" ", normalFont);
        separator.setSpacingBefore(10);
        document.add(separator);

        Paragraph footer = new Paragraph();
        footer.setSpacingBefore(20);
        footer.setAlignment(Element.ALIGN_LEFT);

        Font smallFont = new Font(normalFont.getBaseFont(), 8, Font.NORMAL, Color.GRAY);
        footer.add(new Phrase("生成时间：" + LocalDateTime.now().format(DATE_FORMATTER), smallFont));
        footer.add(Chunk.NEWLINE);
        footer.add(new Phrase("报表ID：" + report.getId(), smallFont));
        footer.add(Chunk.NEWLINE);
        footer.add(new Phrase("本报表由系统自动生成，仅供内部使用", smallFont));

        document.add(footer);
    }

    private static void addInfoRow(PdfPTable table, String label, String value, Font labelFont, Font normalFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setBackgroundColor(new Color(245, 245, 245));

        PdfPCell valueCell = new PdfPCell(new Phrase(value, normalFont));
        valueCell.setBorder(Rectangle.NO_BORDER);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private static BaseFont getChineseFont() throws IOException, DocumentException {
        try {
            return BaseFont.createFont(
                "STSong-Light",
                "UniGB-UCS2-H",
                BaseFont.NOT_EMBEDDED
            );
        } catch (Exception e) {
            log.warn("Failed to load STSong font, trying fallback fonts: {}", e.getMessage());
            try {
                String[] fontPaths = {
                    "C:/Windows/Fonts/simsun.ttc",
                    "C:/Windows/Fonts/msyh.ttc",
                    "/usr/share/fonts/truetype/droid/DroidSansFallbackFull.ttf",
                    "/System/Library/Fonts/PingFang.ttc"
                };
                for (String path : fontPaths) {
                    try {
                        return BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                    } catch (Exception ignored) {}
                }
            } catch (Exception ex) {
                log.error("All font loading attempts failed", ex);
            }
            return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.EMBEDDED);
        }
    }

    private static String getReportTypeName(String reportType) {
        if (reportType == null) return "工作报表";
        switch (reportType.toUpperCase()) {
            case "DAILY": return "日报";
            case "WEEKLY": return "周报";
            case "MONTHLY": return "月报";
            default: return "工作报表";
        }
    }

    private static String getStatusName(String status) {
        if (status == null) return "未知";
        switch (status.toUpperCase()) {
            case "DRAFT": return "草稿";
            case "SUBMITTED": return "已提交";
            case "APPROVED": return "已通过";
            case "REJECTED": return "已驳回";
            default: return status;
        }
    }

    private static String getPostTypeName(SysUser user) {
        if (user == null || user.getPostType() == null) return "-";
        switch (user.getPostType().toUpperCase()) {
            case "DEV": return "研发";
            case "OPS": return "运维";
            case "CS": return "客服";
            default: return user.getPostType();
        }
    }

    private static String formatPeriod(String period, String reportType) {
        if (period == null) return "-";
        try {
            if ("DAILY".equalsIgnoreCase(reportType)) {
                LocalDate date = LocalDate.parse(period);
                return date.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
            } else if ("WEEKLY".equalsIgnoreCase(reportType)) {
                return period.replace("-", "年第") + "周";
            } else if ("MONTHLY".equalsIgnoreCase(reportType)) {
                String[] parts = period.split("-");
                return parts[0] + "年" + parts[1] + "月";
            }
        } catch (Exception e) {
            log.warn("Failed to parse period: {}", period);
        }
        return period;
    }
}
