DELETE FROM sequencias_de_estudos;

INSERT INTO sequencias_de_estudos (aluno, dias_consecutivos, moedas_conquistadas)
SELECT 'aluno-' || lpad(g::text, 6, '0'), (g * 7) % 31, (g * 13) % 52
FROM generate_series(1, 100000) AS g;

ANALYZE sequencias_de_estudos;

SELECT count(*) AS alunos FROM sequencias_de_estudos;
