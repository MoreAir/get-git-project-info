package com.moreair;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;

@Slf4j
@Component
public class DownloadService {

    @Value("${outputFilePath}")
    private String outputFilePath;

    @Value("${cookie}")
    private String cookie;

    @Value("${baseUrl}")
    private String baseUrl;

    @PostConstruct
    public void core() throws Exception {
        List<ColumnContent> data = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            getUrl(i,data);
        }
        outputFilePath=outputFilePath+"\\"+System.currentTimeMillis()+".xlsx";
        EasyExcel.write(outputFilePath, ColumnContent.class).sheet("代码信息").doWrite(data);
        log.info("Excel 文件已导出到: " + outputFilePath);
    }

    private void getUrl(int page,List<ColumnContent> data) throws IOException {
        Connection connect = Jsoup.connect(baseUrl+"/admin/projects?page=" + page + "&sort=latest_activity_desc");
        Map headerMap = new HashMap();
        headerMap.put("cookie", cookie);
        connect.headers(headerMap);
        Document document = connect.get();
        Elements projectRows = document.select(".title");
        log.info("当前数量={}",projectRows.size());
        for (Element projectRow : projectRows) {
            try {
                Element aTag = projectRow.selectFirst("a");
                String href = aTag.attr("href").replace("/admin/projects", "");
                String projectName = getProjectName(aTag, href);
                String codeUrl=baseUrl + href;
                String content = getDescription(headerMap, codeUrl);
                if(href!=null&&href.length()>0){
                    data.add(new ColumnContent(projectName,codeUrl, content));
                }
            } catch (Exception e) {
                log.error("获取异常",e);
            }
        }
    }

    private String getDescription(Map headerMap, String codeUrl) throws IOException {
        Connection connectDesc = Jsoup.connect(codeUrl);
        connectDesc.headers(headerMap);
        Document document1 = connectDesc.get();
        Element metaDescription = document1.selectFirst("meta[name=description]");
        String content = null;
        if (metaDescription != null) {
            content = metaDescription.attr("content");
            log.info("Description: " + content);
        } else {
            log.info("Meta description not found.");
        }
        return content;
    }

    private String getProjectName(Element a, String href) {
        Element projectNameSpan = a.selectFirst("span.project-name");
        String projectName = null;
        if (projectNameSpan != null) {
            // 获取project-name的文本
            projectName = projectNameSpan.text();
            log.info("Project Name: " + projectName + " href:" + href);
        } else {
            log.info("Project name not found.");
        }
        return projectName;
    }

    /**
     * 输出列信息
     */
    @Data
    public static class ColumnContent {
        @ExcelProperty("名称")
        private String projectName;
        @ExcelProperty("代码路径链接")
        private String url;
        @ExcelProperty("备注")
        private String remark;
        public ColumnContent(String projectName,String url, String remark) {
            this.url = url;
            this.projectName = projectName;
            this.remark = remark;
        }
    }

}
