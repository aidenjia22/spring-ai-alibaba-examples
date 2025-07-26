package com.alibaba.cloud.ai.toolcall.component;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.stereotype.Service;

@Service
public class BaiduSearchService {
    private static final String BAIDU_SEARCH_URL = "http://www.baidu.com/s?tn=baidu&ie=utf-8&wd=";
    private static final int MAX_RESULT_LENGTH = 12000; // 限制返回结果的最大长度
    public String search(String query) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(BAIDU_SEARCH_URL + java.net.URLEncoder.encode(query, "UTF-8") + "&wdmm=1&tn=SE_Pclog_SearchButton&sc=utf-8&wordlength=all&rsv_bp=1&rsv_spt=3&rsv_sug3=9&rsv_sug1=9&rsv_sug7=101");
            request.addHeader("User-Agent", "Mozilla/5.0");
            // 这里返回的可能是 HTML，需要根据实际需要处理或解析数据
            String result =  EntityUtils.toString(client.execute(request).getEntity());
            if (result.length() > MAX_RESULT_LENGTH) {
                return result.substring(0, MAX_RESULT_LENGTH);
            }
            return  result;
        } catch (Exception e) {
            return "Error occurred: " + e.getMessage();
        }
    }
}
