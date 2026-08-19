UPDATE techlabel_reg_v1
SET section = CASE
                  WHEN label ~* 'sete'    THEN 'Sete'
                  WHEN label ~* 'armlene' THEN 'Armlene'
                  WHEN label ~* 'rygg'    THEN 'Rygg'
                  WHEN lower(trim(unit)) IN ('cm', 'tommer', 'kg', 'gram', 'g') THEN 'Mål og vekt'
                  WHEN label ~* 'batteri' THEN 'Batteri'
                  WHEN lower(trim(unit)) IN ('volt', 'v', 't', 'ah') THEN 'Batteri'
    END,
    updated = now(),
    updated_by_user = 'system'
WHERE section IS DISTINCT FROM (
    CASE
        WHEN label ~* 'sete'    THEN 'Sete'
        WHEN label ~* 'armlene' THEN 'Armlene'
        WHEN label ~* 'rygg'    THEN 'Rygg'
        WHEN lower(trim(unit)) IN ('cm', 'tommer', 'kg', 'gram', 'g') THEN 'Mål og vekt'
        WHEN label ~* 'batteri' THEN 'Batteri'
        WHEN lower(trim(unit)) IN ('volt', 'v', 't', 'ah') THEN 'Batteri'
        END
    );
