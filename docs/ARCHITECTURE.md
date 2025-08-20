# Architecture

```mermaid
flowchart LR
  UI[HTTP Client
/Postman/React] --> API[Spring Boot API
POST /api/score]
  subgraph Services
    API --> ResumeSvc[ResumeService
(PDF/DOCX -> text via Apache Tika)]
    API --> JobFetch[JobFetcher
(JSoup fetch & clean HTML)]
    API --> Agent[ScoringAgent
(Gemini on Vertex AI)]
    Agent -->|prompt| Gemini[(Vertex AI
Gemini 2.5 Flash)]
    API --> BQ[BigQueryService
(InsertAll streaming)]
  end

  subgraph GCP
    Gemini
    BQ[(BigQuery
candidate_scores)]
    CR[Cloud Run]
  end

  API --> CR
  BQ -. query/BI .- BI[Looker Studio / SQL]
```
