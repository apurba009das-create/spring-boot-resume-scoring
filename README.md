# TalentMatch Agentic (Java Spring Boot + Google Cloud)

An **agentic AI** reference project that:
- Accepts a resume upload (PDF/DOCX) and a LinkedIn (or any) job URL.
- Uses **Gemini** on **Vertex AI** to extract job requirements and score the resume against the job (0–100) with subscores and rationale.
- Stores `{candidate_name, candidate_email, job_url, score, subscores_json, created_at}` in **BigQuery**.
- Designed for deployment on **Cloud Run**.

## Quickstart

### 1) Prereqs
- Java 17+, Maven 3.9+
- A Google Cloud project with billing enabled
- Enable APIs: Vertex AI API, BigQuery API
- Create a service account with roles: `roles/aiplatform.user`, `roles/bigquery.dataEditor` (or finer-grained), `roles/serviceusage.serviceUsageConsumer`.
- **Auth**: For local dev, either set `GOOGLE_APPLICATION_CREDENTIALS=/path/to/key.json`, or use `gcloud auth application-default login`.
- For Vertex AI SDK configuration, set environment variables (either option):
  - **Project/Location mode** (recommended for Cloud Run):
    ```bash
    export GOOGLE_GENAI_USE_VERTEXAI=true
    export GOOGLE_CLOUD_PROJECT=<your-project-id>
    export GOOGLE_CLOUD_LOCATION=us-central1
    ```
  - **Express mode** (API key):
    ```bash
    export GOOGLE_GENAI_USE_VERTEXAI=true
    export GOOGLE_API_KEY=<vertex-express-api-key>
    ```

### 2) BigQuery table
Create dataset and table:
```sql
-- scripts/setup_bq.sql
CREATE SCHEMA IF NOT EXISTS `talent_analytics`;

CREATE TABLE IF NOT EXISTS `talent_analytics.candidate_scores` (
  created_at TIMESTAMP NOT NULL,
  candidate_name STRING,
  candidate_email STRING,
  job_url STRING,
  score FLOAT64,
  subscores_json STRING,
  notes STRING
);
```
Set environment for app:
```bash
export BQ_DATASET=talent_analytics
export BQ_TABLE=candidate_scores
```

### 3) Run locally
```bash
./mvnw spring-boot:run
# or
mvn spring-boot:run
```
Open Swagger UI at `http://localhost:8080/swagger-ui/index.html`

### 4) Build & Dockerize
```bash
mvn -DskipTests package
docker build -t gcr.io/$GOOGLE_CLOUD_PROJECT/talentmatch-agentic:latest .
```

### 5) Deploy to Cloud Run
```bash
gcloud run deploy talentmatch-agentic \
  --source . \
  --region us-central1 \
  --allow-unauthenticated \
  --set-env-vars BQ_DATASET=$BQ_DATASET,BQ_TABLE=$BQ_TABLE \
  --set-env-vars GOOGLE_GENAI_USE_VERTEXAI=true,GOOGLE_CLOUD_PROJECT=$GOOGLE_CLOUD_PROJECT,GOOGLE_CLOUD_LOCATION=us-central1
```

## API
`POST /api/score` (multipart/form-data)
- `resume` (file, required): PDF/DOCX
- `jobUrl` (string, required)
- `candidateName` (string, required)
- `candidateEmail` (string, required)

**Response**
```json
{
  "score": 87.5,
  "subscores": {
    "skills": 90,
    "experience": 80,
    "education": 85,
    "keywordsCoverage": 95
  },
  "explanation": "Short rationale..."
}
```

> ⚠️ Note on LinkedIn: Some job pages block automated scrapers. The server uses a best-effort fetch; if blocked, provide a non-protected URL or paste the job description.

## Architecture
See `docs/ARCHITECTURE.md` for the diagram and data flow.

## License
Apache-2.0
