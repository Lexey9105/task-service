CREATE TABLE task_table (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 task_name VARCHAR(255) NOT NULL,
 performer_id UUID,
 description TEXT,
 status VARCHAR(32) NOT NULL
    CHECK (status IN ('NEW', 'IN_PROGRESS', 'DONE', 'HOLD')),
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT fk_task_performer
    FOREIGN KEY (performer_id)
    REFERENCES user_table(id)
);

CREATE INDEX idx_task_performer_id ON task_table(performer_id);
CREATE INDEX idx_task_created_at ON task_table(created_at);
