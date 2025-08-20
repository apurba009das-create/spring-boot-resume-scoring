package com.talentmatch.model;

import org.springframework.web.multipart.MultipartFile;

public class CandidateRequest {
    private MultipartFile resume;
    private String jobUrl;
    private String candidateName;
    private String candidateEmail;

    public MultipartFile getResume() { return resume; }
    public void setResume(MultipartFile resume) { this.resume = resume; }

    public String getJobUrl() { return jobUrl; }
    public void setJobUrl(String jobUrl) { this.jobUrl = jobUrl; }

    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String candidateName) { this.candidateName = candidateName; }

    public String getCandidateEmail() { return candidateEmail; }
    public void setCandidateEmail(String candidateEmail) { this.candidateEmail = candidateEmail; }
}
