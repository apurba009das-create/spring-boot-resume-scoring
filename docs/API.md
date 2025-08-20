# API Details

## POST /api/score
Consumes: `multipart/form-data`

### Parameters
- `resume`: file (PDF/DOCX), required
- `jobUrl`: string, required
- `candidateName`: string, required
- `candidateEmail`: string, required

### Response
JSON with fields: `score`, `subscores`, `explanation`.
