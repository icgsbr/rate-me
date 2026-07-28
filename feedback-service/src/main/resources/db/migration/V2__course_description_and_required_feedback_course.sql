-- ============================================================================
-- V2 - courses become a first-class entity
--
-- Feedback used to be attached to a single seeded course (app.default-course-id).
-- Courses are now registered through POST /courses and the submission carries the
-- courseId, so the column stops being optional.
-- ============================================================================

ALTER TABLE course ADD COLUMN description VARCHAR(1000);

-- The V1 seed is where the pre-existing feedback is parked; recreated defensively in
-- case it was removed from the shared database.
INSERT INTO course (id, name)
VALUES ('00000000-0000-0000-0000-000000000001', 'General')
ON CONFLICT (id) DO NOTHING;

UPDATE course
SET description = 'Curso geral herdado do modelo de curso unico'
WHERE id = '00000000-0000-0000-0000-000000000001'
  AND description IS NULL;

-- Backfill before tightening the constraint, so no historical row is lost.
UPDATE feedback
SET course_id = '00000000-0000-0000-0000-000000000001'
WHERE course_id IS NULL;

ALTER TABLE feedback ALTER COLUMN course_id SET NOT NULL;

CREATE INDEX idx_feedback_course_id ON feedback (course_id);
