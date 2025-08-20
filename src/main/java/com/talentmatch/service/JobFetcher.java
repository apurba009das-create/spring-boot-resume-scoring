package com.talentmatch.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

@Service
public class JobFetcher {
    public String fetchJobDescription(String url) {
        try {
            // Some job boards block bots; this is best-effort and may fail.
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125 Safari/537.36")
                    .referrer("https://www.google.com/")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .timeout(15000)
                    .get();
            // Simple heuristic: prefer main content text
            String text = doc.select("main").text();
            if (text == null || text.isBlank()) {
                text = doc.body().text();
            }
            // Truncate to a sane length for prompting
            return text.length() > 10000 ? text.substring(0, 10000) : text;
        } catch (Exception e) {
            return "";
        }
    }
}
