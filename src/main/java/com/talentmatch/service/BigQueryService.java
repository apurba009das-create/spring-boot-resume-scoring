package com.talentmatch.service;

import com.google.cloud.bigquery.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class BigQueryService {

    @Value("${app.bigquery.dataset}")
    private String dataset;

    @Value("${app.bigquery.table}")
    private String table;

    private final BigQuery bigQuery = BigQueryOptions.getDefaultInstance().getService();

    public void insertScore(String name, String email, String jobUrl, double score, String subscoresJson, String notes) {
        TableId tableId = TableId.of(dataset, table);
        Map<String, Object> row = new HashMap<>();
        row.put("created_at", Instant.now().toString());
        row.put("candidate_name", name);
        row.put("candidate_email", email);
        row.put("job_url", jobUrl);
        row.put("score", score);
        row.put("subscores_json", subscoresJson);
        row.put("notes", notes);

        InsertAllRequest request = InsertAllRequest.newBuilder(tableId)
                .addRow(row)
                .build();
        InsertAllResponse response = bigQuery.insertAll(request);
        if (response.hasErrors()) {
            throw new RuntimeException("BigQuery insert errors: " + response.getInsertErrors().toString());
        }
    }
}
