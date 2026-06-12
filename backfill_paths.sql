-- Recursive CTE to backfill materialized paths for existing folders
-- This is a one-time migration script.

WITH RECURSIVE folder_tree AS (
    -- Base case: Root folders (parent_id IS NULL)
    SELECT 
        id, 
        parent_id, 
        '/' || id || '/' AS new_path
    FROM folders
    WHERE parent_id IS NULL
    
    UNION ALL
    
    -- Recursive step: Child folders
    SELECT 
        f.id, 
        f.parent_id, 
        ft.new_path || f.id || '/' AS new_path
    FROM folders f
    INNER JOIN folder_tree ft ON f.parent_id = ft.id
)
UPDATE folders f
SET path = ft.new_path
FROM folder_tree ft
WHERE f.id = ft.id;
