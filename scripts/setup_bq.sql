-- Create dataset and table for candidate scoring
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
