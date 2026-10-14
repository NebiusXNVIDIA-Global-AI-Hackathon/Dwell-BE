-- Legal RAG: the corpus (resources/legal/legal_corpus.yaml) is synced at startup.
-- One yaml entry = one legal_document + one legal_chunk. Retrieval filters by issue type / usage (no embedding model yet).

ALTER TABLE legal_document
    ADD COLUMN code        VARCHAR(50) NOT NULL UNIQUE, -- yaml id, e.g. NYC-HMC-27-2005 (the id the LLM cites)
    ADD COLUMN issue_types JSONB       NOT NULL,        -- e.g. ["leak", "mold"] or ["all"]
    ADD COLUMN used_by     JSONB       NOT NULL,        -- e.g. ["notice", "assistant"]
    ADD COLUMN text_type   VARCHAR(20) NOT NULL,        -- VERBATIM | SUMMARY
    ADD COLUMN summary     TEXT        NOT NULL;

ALTER TABLE legal_chunk
    ALTER COLUMN embedding DROP NOT NULL; -- filled once an embedding model is available
