package com.talentmatch.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

@Service
public class ScoringAgent {
    private static final Logger LOG = Logger.getLogger(ScoringAgent.class.getName());
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final ObjectMapper mapper = new ObjectMapper();
    private final OkHttpClient http = new OkHttpClient();

    @Value("${app.llm.model:gemini-1.5-flash}")
    private String model;

    @Value("${app.llm.apiKey:#{null}}")
    private String apiKey; // OR set via env var GOOGLE_API_KEY

    public Result score(String resumeText, String jobText) throws Exception {
        if (apiKey == null || apiKey.isBlank()) apiKey = System.getenv("GOOGLE_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Missing Google API key. Set env GOOGLE_API_KEY or app.llm.apiKey");
        }

        // Strong schema instruction
        String schemaInstr =
                "Return ONLY one JSON object (no markdown, no code fences) with EXACT schema:\n" +
                        "{ \"score\": <float 0-100>, " +
                        "  \"subscores\": { \"skills\": <float>, \"experience\": <float>, \"education\": <float>, \"keywordsCoverage\": <float> }, " +
                        "  \"explanation\": <string> }";

        String userPrompt =
                "JOB DESCRIPTION:\n" + snippet(jobText, 6000) +
                        "\n\nRESUME:\n" + snippet(resumeText, 6000) +
                        "\n\n" + schemaInstr;

        // ---- Attempt #1: force JSON via generationConfig.response_mime_type ----
        String text1 = callGemini(userPrompt, /*forceJson*/ true);
        String jsonStr = extractFirstJsonObject(stripTripleBackticks(text1));

        // ---- Attempt #2: if still not JSON, ask model to reformat to JSON ONLY ----
        if (jsonStr == null || jsonStr.isBlank()) {
            LOG.warning("LLM RAW (attempt #1 not JSON):\n" + safePreview(text1));
            String repairPrompt =
                    "Convert the following content into VALID JSON ONLY that matches this exact schema:\n" +
                            "{ \"score\": <float 0-100>, " +
                            "  \"subscores\": { \"skills\": <float>, \"experience\": <float>, \"education\": <float>, \"keywordsCoverage\": <float> }, " +
                            "  \"explanation\": <string> }\n" +
                            "Do not include any extra text, comments, or code fences.\n\n" +
                            "CONTENT:\n" + text1;
            String text2 = callGemini(repairPrompt, /*forceJson*/ true);
            jsonStr = extractFirstJsonObject(stripTripleBackticks(text2));

            if (jsonStr == null || jsonStr.isBlank()) {
                LOG.warning("LLM RAW (attempt #2 not JSON):\n" + safePreview(text2));
                throw new IllegalArgumentException("Model did not return a JSON object.");
            }
        }

        JsonNode json = mapper.readTree(jsonStr);

        double score = json.path("score").asDouble();
        Map<String, Double> subscores = new HashMap<>();
        JsonNode subs = json.path("subscores");
        if (subs.isObject()) {
            Iterator<String> it = subs.fieldNames();
            while (it.hasNext()) {
                String k = it.next();
                subscores.put(k, subs.path(k).asDouble());
            }
        }
        String explanation = json.path("explanation").asText();

        return new Result(score, subscores, explanation, jsonStr);
    }

    /** Single call to Gemini. If forceJson=true we set response_mime_type=application/json to make it return pure JSON. */
    private String callGemini(String prompt, boolean forceJson) throws IOException {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

        Map<String, Object> req = new HashMap<>();
        if (forceJson) {
            Map<String, Object> genCfg = new HashMap<>();
            genCfg.put("response_mime_type", "application/json");
            genCfg.put("temperature", 0.2);
            genCfg.put("topP", 0.9);
            genCfg.put("topK", 40);
            req.put("generationConfig", genCfg);
        }
        // Put the instruction directly in the user message (reliable across API variants)
        Map<String, Object> userContent = new HashMap<>();
        userContent.put("role", "user");
        userContent.put("parts", List.of(Map.of("text", prompt)));
        req.put("contents", List.of(userContent));

        String payload = mapper.writeValueAsString(req);
        RequestBody body = RequestBody.create(payload, JSON);
        Request request = new Request.Builder().url(url).post(body).build();

        try (Response response = http.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Gemini API error: " + response.code() + " " + response.message());
            }
            String resp = Objects.requireNonNull(response.body()).string();
            // Safely navigate to candidates[0].content.parts[0].text
            JsonNode root = mapper.readTree(resp);
            JsonNode parts0 = root.path("candidates").path(0).path("content").path("parts").path(0);
            String text = parts0.path("text").asText(null);

            // Some responses may nest further or return empty text; fall back to toString
            if (text == null || text.isBlank()) {
                text = parts0.toString();
            }
            return text == null ? "" : text.trim();
        }
    }

    private String snippet(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) : s;
    }

    private String safePreview(String s) {
        if (s == null) return "";
        String t = s.replace("\r", " ").replace("\n", " ");
        return t.length() > 600 ? t.substring(0, 600) + " …(truncated)" : t;
    }

    /** Remove ``` or ```json fences if present. */
    private String stripTripleBackticks(String s) {
        if (s == null) return "";
        s = s.trim();
        if (s.startsWith("```")) {
            int nl = s.indexOf('\n');
            if (nl > 0) s = s.substring(nl + 1);
            int fence = s.lastIndexOf("```");
            if (fence >= 0) s = s.substring(0, fence);
        }
        return s.trim();
    }

    /** Extract the first balanced JSON object in a string (handles nested braces and quoted strings). */
    private String extractFirstJsonObject(String s) {
        if (s == null) return null;
        int start = s.indexOf('{');
        if (start < 0) return null;

        boolean inString = false, escape = false;
        int depth = 0;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (escape) { escape = false; continue; }
            if (c == '\\') { if (inString) escape = true; continue; }
            if (c == '"') { inString = !inString; continue; }
            if (!inString) {
                if (c == '{') depth++;
                else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        return s.substring(start, i + 1).trim();
                    }
                }
            }
        }
        return null;
    }

    public static class Result {
        public final double score;
        public final Map<String, Double> subscores;
        public final String explanation;
        public final String rawJson;

        public Result(double score, Map<String, Double> subscores, String explanation, String rawJson) {
            this.score = score;
            this.subscores = subscores;
            this.explanation = explanation;
            this.rawJson = rawJson;
        }
    }
}
