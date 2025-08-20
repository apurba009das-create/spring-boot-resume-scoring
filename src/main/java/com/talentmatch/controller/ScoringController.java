package com.talentmatch.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentmatch.model.ScoreResult;
import com.talentmatch.service.BigQueryService;
import com.talentmatch.service.JobFetcher;
import com.talentmatch.service.ResumeService;
import com.talentmatch.service.ScoringAgent;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api")
@Validated
public class ScoringController {

    private final ResumeService resumeService;
    private final JobFetcher jobFetcher;
    private final ScoringAgent scoringAgent;
    private final BigQueryService bigQueryService;
    private final ObjectMapper mapper = new ObjectMapper();

    public ScoringController(ResumeService resumeService, JobFetcher jobFetcher,
                             ScoringAgent scoringAgent, BigQueryService bigQueryService) {
        this.resumeService = resumeService;
        this.jobFetcher = jobFetcher;
        this.scoringAgent = scoringAgent;
        this.bigQueryService = bigQueryService;
    }

    @PostMapping(value = "/score", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ScoreResult> score(
            @RequestPart("resume") MultipartFile resume,
            @RequestPart("jobUrl") @NotBlank String jobUrl,
            @RequestPart(value = "jobText", required = false) String jobText,
            @RequestPart("candidateName") @NotBlank String candidateName,
            @RequestPart("candidateEmail") @Email String candidateEmail
    ) throws Exception {

        String resumeText = resumeService.extractText(resume);

        // Fallback: use provided jobText if URL is blocked
        String jobTextResolved = (jobText != null && !jobText.isBlank())
                ? jobText
                : jobFetcher.fetchJobDescription(jobUrl);

        if (jobTextResolved == null || jobTextResolved.isBlank()) {
            throw new IllegalArgumentException("Could not fetch job description from the URL and no jobText was provided.");
        }

        ScoringAgent.Result res = scoringAgent.score(resumeText, jobTextResolved);

        bigQueryService.insertScore(
                candidateName, candidateEmail, jobUrl,
                res.score, res.rawJson, res.explanation
        );

        ScoreResult response = new ScoreResult(res.score, res.subscores, res.explanation);
        return ResponseEntity.ok(response);
    }
}
