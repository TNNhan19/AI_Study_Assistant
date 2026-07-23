-- Kết quả mong đợi: các query "missing" đều trả về 0 dòng.

-- 1. Kiểm tra các bảng foundation và trạng thái RLS.
with expected_tables(table_name) as (
    values
        ('users'), ('projects'), ('topics'), ('documents'), ('summaries'),
        ('flashcards'), ('quizzes'), ('quiz_sets'), ('notes'), ('study_plans'),
        ('reminders'), ('chat_history')
)
select
    expected_tables.table_name,
    coalesce(pg_class.relrowsecurity, false) as rls_enabled
from expected_tables
left join (
    select pg_class.relname, pg_class.relrowsecurity
    from pg_class
    join pg_namespace on pg_namespace.oid = pg_class.relnamespace
    where pg_namespace.nspname = 'public'
) as pg_class on pg_class.relname = expected_tables.table_name
order by expected_tables.table_name;

-- 2. Tìm cột bắt buộc còn thiếu trong database hiện tại.
with expected_columns(table_name, column_name) as (
    values
        ('users', 'id'), ('users', 'email'),
        ('projects', 'id'), ('projects', 'user_id'), ('projects', 'name'),
        ('topics', 'id'), ('topics', 'user_id'), ('topics', 'project_id'),
        ('topics', 'name'), ('topics', 'is_pinned'),
        ('documents', 'id'), ('documents', 'user_id'),
        ('documents', 'project_id'), ('documents', 'topic_id'),
        ('documents', 'name'), ('documents', 'file_path'),
        ('documents', 'file_type'), ('documents', 'file_size'),
        ('documents', 'status'), ('documents', 'is_favorite'),
        ('summaries', 'id'), ('summaries', 'user_id'),
        ('summaries', 'document_id'), ('summaries', 'summary_text'),
        ('flashcards', 'id'), ('flashcards', 'user_id'),
        ('flashcards', 'document_id'), ('flashcards', 'front'),
        ('flashcards', 'back'),
        ('quizzes', 'id'), ('quizzes', 'user_id'),
        ('quizzes', 'document_id'), ('quizzes', 'quiz_set_id'),
        ('quizzes', 'question'),
        ('quiz_sets', 'id'), ('quiz_sets', 'user_id'),
        ('quiz_sets', 'document_id'), ('quiz_sets', 'difficulty'),
        ('notes', 'id'), ('notes', 'user_id'), ('notes', 'title'),
        ('notes', 'is_pinned'),
        ('study_plans', 'id'), ('study_plans', 'user_id'),
        ('reminders', 'id'), ('reminders', 'user_id'),
        ('chat_history', 'id'), ('chat_history', 'user_id'),
        ('chat_history', 'project_id'), ('chat_history', 'topic_id'),
        ('chat_history', 'document_id'), ('chat_history', 'role'),
        ('chat_history', 'content'), ('chat_history', 'created_at')
)
select expected_columns.table_name, expected_columns.column_name as missing_column
from expected_columns
left join information_schema.columns
    on information_schema.columns.table_schema = 'public'
    and information_schema.columns.table_name = expected_columns.table_name
    and information_schema.columns.column_name = expected_columns.column_name
where information_schema.columns.column_name is null
order by expected_columns.table_name, expected_columns.column_name;

-- 3. Tìm CRUD policy còn thiếu trên các bảng có user_id.
with target_tables(table_name) as (
    values
        ('projects'), ('topics'), ('documents'), ('summaries'), ('flashcards'),
        ('quizzes'), ('quiz_sets'), ('notes'), ('study_plans'),
        ('reminders'), ('chat_history')
), required_commands(cmd) as (
    values ('SELECT'), ('INSERT'), ('UPDATE'), ('DELETE')
)
select target_tables.table_name, required_commands.cmd as missing_policy
from target_tables
cross join required_commands
left join pg_policies
    on pg_policies.schemaname = 'public'
    and pg_policies.tablename = target_tables.table_name
    and pg_policies.cmd = required_commands.cmd
where pg_policies.policyname is null
order by target_tables.table_name, required_commands.cmd;

-- 4. Review điều kiện của tất cả policy foundation.
select policyname, tablename, cmd, roles, qual, with_check
from pg_policies
where schemaname = 'public'
  and tablename in (
      'users', 'projects', 'topics', 'documents', 'summaries', 'flashcards',
      'quizzes', 'quiz_sets', 'notes', 'study_plans', 'reminders', 'chat_history'
  )
order by tablename, cmd;

-- 5. Kiểm tra trigger đồng bộ user và updated_at.
select
    event_object_schema,
    event_object_table,
    trigger_name,
    action_timing,
    event_manipulation
from information_schema.triggers
where trigger_name = 'on_auth_user_created'
   or trigger_name like 'set_%_updated_at'
order by event_object_schema, event_object_table, trigger_name;

-- 6. Bucket documents phải có public = false.
select id, name, public, file_size_limit, allowed_mime_types
from storage.buckets
where id = 'documents';

-- 7. Storage phải có đủ SELECT, INSERT, UPDATE, DELETE policy.
with required_commands(cmd) as (
    values ('SELECT'), ('INSERT'), ('UPDATE'), ('DELETE')
)
select required_commands.cmd as missing_storage_policy
from required_commands
left join pg_policies
    on pg_policies.schemaname = 'storage'
    and pg_policies.tablename = 'objects'
    and pg_policies.policyname = 'documents_' || lower(required_commands.cmd) || '_own'
where pg_policies.policyname is null
order by required_commands.cmd;

-- 8. Kiểm tra account Auth nào chưa được đồng bộ sang public.users.
select auth.users.id, auth.users.email
from auth.users
left join public.users on public.users.id = auth.users.id
where public.users.id is null;
