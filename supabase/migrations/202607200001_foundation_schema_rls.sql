-- AI Study Assistant - schema và bảo mật nền tảng.
-- Chạy file này trong Supabase SQL Editor bằng tài khoản quản trị dự án.

begin;

create extension if not exists pgcrypto with schema extensions;

-- Tài khoản ứng dụng: id trùng với auth.users.id.
create table if not exists public.users (
    id uuid primary key references auth.users(id) on delete cascade,
    email text not null unique,
    full_name text,
    avatar_url text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists public.projects (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references public.users(id) on delete cascade,
    name text not null,
    description text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists public.topics (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references public.users(id) on delete cascade,
    project_id uuid references public.projects(id) on delete set null,
    name text not null,
    description text,
    is_pinned boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists public.documents (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references public.users(id) on delete cascade,
    project_id uuid references public.projects(id) on delete set null,
    topic_id uuid references public.topics(id) on delete set null,
    name text not null,
    file_path text not null,
    file_type text not null check (file_type in ('pdf', 'doc', 'docx', 'txt')),
    file_size bigint not null default 0 check (file_size >= 0),
    status text not null default 'UPLOADED'
        check (status in ('UPLOADED', 'PROCESSING', 'COMPLETED', 'FAILED')),
    is_favorite boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists public.summaries (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references public.users(id) on delete cascade,
    document_id uuid not null references public.documents(id) on delete cascade,
    summary_text text not null,
    key_points jsonb not null default '[]'::jsonb,
    keywords jsonb not null default '[]'::jsonb,
    conclusion text,
    created_at timestamptz not null default now()
);

create table if not exists public.flashcards (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references public.users(id) on delete cascade,
    document_id uuid not null references public.documents(id) on delete cascade,
    topic_id uuid references public.topics(id) on delete set null,
    front text not null,
    back text not null,
    difficulty text not null default 'MEDIUM'
        check (difficulty in ('EASY', 'MEDIUM', 'HARD', 'FORGOT')),
    next_review_at timestamptz,
    created_at timestamptz not null default now()
);

-- Mỗi record trong quizzes đại diện cho một câu hỏi trắc nghiệm.
create table if not exists public.quizzes (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references public.users(id) on delete cascade,
    document_id uuid not null references public.documents(id) on delete cascade,
    topic_id uuid references public.topics(id) on delete set null,
    question text not null,
    option_a text not null,
    option_b text not null,
    option_c text not null,
    option_d text not null,
    correct_answer text not null check (correct_answer in ('A', 'B', 'C', 'D')),
    explanation text,
    difficulty text not null default 'MEDIUM'
        check (difficulty in ('EASY', 'MEDIUM', 'HARD')),
    created_at timestamptz not null default now()
);

create table if not exists public.notes (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references public.users(id) on delete cascade,
    document_id uuid references public.documents(id) on delete set null,
    topic_id uuid references public.topics(id) on delete set null,
    title text not null,
    content text,
    is_pinned boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists public.study_plans (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references public.users(id) on delete cascade,
    project_id uuid references public.projects(id) on delete set null,
    title text not null,
    exam_date date,
    plan_data jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists public.reminders (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references public.users(id) on delete cascade,
    study_plan_id uuid references public.study_plans(id) on delete cascade,
    title text not null,
    description text,
    reminder_at timestamptz not null,
    is_completed boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists public.chat_history (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references public.users(id) on delete cascade,
    project_id uuid references public.projects(id) on delete set null,
    topic_id uuid references public.topics(id) on delete set null,
    document_id uuid references public.documents(id) on delete cascade,
    role text not null check (role in ('USER', 'ASSISTANT', 'SYSTEM')),
    content text not null,
    created_at timestamptz not null default now()
);

-- Index cho các filter được Repository sử dụng thường xuyên.
create index if not exists idx_projects_user_id on public.projects(user_id);
create index if not exists idx_topics_user_id on public.topics(user_id);
create index if not exists idx_topics_project_id on public.topics(project_id);
create index if not exists idx_documents_user_id on public.documents(user_id);
create index if not exists idx_documents_project_id on public.documents(project_id);
create index if not exists idx_documents_topic_id on public.documents(topic_id);
create index if not exists idx_summaries_user_id on public.summaries(user_id);
create index if not exists idx_summaries_document_id on public.summaries(document_id);
create index if not exists idx_flashcards_user_id on public.flashcards(user_id);
create index if not exists idx_flashcards_document_id on public.flashcards(document_id);
create index if not exists idx_quizzes_user_id on public.quizzes(user_id);
create index if not exists idx_quizzes_document_id on public.quizzes(document_id);
create index if not exists idx_notes_user_id on public.notes(user_id);
create index if not exists idx_study_plans_user_id on public.study_plans(user_id);
create index if not exists idx_reminders_user_id on public.reminders(user_id);
create index if not exists idx_chat_history_user_id on public.chat_history(user_id);

-- Tự động cập nhật updated_at cho các bảng có cột này.
create or replace function public.set_updated_at()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

do $$
declare
    target_table text;
begin
    foreach target_table in array array[
        'users', 'projects', 'topics', 'documents', 'notes',
        'study_plans', 'reminders'
    ]
    loop
        -- Database cũ thiếu updated_at vẫn chạy migration an toàn.
        if exists (
            select 1
            from information_schema.columns
            where table_schema = 'public'
              and table_name = target_table
              and column_name = 'updated_at'
        ) then
            execute format(
                'drop trigger if exists %I on public.%I',
                'set_' || target_table || '_updated_at',
                target_table
            );
            execute format(
                'create trigger %I before update on public.%I for each row execute function public.set_updated_at()',
                'set_' || target_table || '_updated_at',
                target_table
            );
        end if;
    end loop;
end;
$$;

-- Đồng bộ account mới từ Supabase Auth sang public.users.
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    insert into public.users (id, email, full_name, created_at)
    values (
        new.id,
        new.email,
        coalesce(nullif(new.raw_user_meta_data ->> 'full_name', ''), 'Student'),
        now()
    )
    on conflict (id) do update
    set email = excluded.email,
        full_name = coalesce(public.users.full_name, excluded.full_name);

    return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
after insert on auth.users
for each row execute function public.handle_new_user();

-- Backfill account đã tồn tại trước khi trigger được tạo.
insert into public.users (id, email, full_name, created_at)
select
    id,
    email,
    coalesce(nullif(raw_user_meta_data ->> 'full_name', ''), 'Student'),
    created_at
from auth.users
where email is not null
on conflict (id) do update
set email = excluded.email,
    full_name = coalesce(public.users.full_name, excluded.full_name);

-- RLS cho profile: client chỉ đọc và sửa profile của chính mình.
alter table public.users enable row level security;

revoke all on table public.users from anon;
grant select, update on table public.users to authenticated;

do $$
declare
    policy_record record;
begin
    for policy_record in
        select policyname
        from pg_policies
        where schemaname = 'public' and tablename = 'users'
    loop
        execute format('drop policy %I on public.users', policy_record.policyname);
    end loop;
end;
$$;

create policy users_select_own
on public.users for select to authenticated
using ((select auth.uid()) = id);

create policy users_update_own
on public.users for update to authenticated
using ((select auth.uid()) = id)
with check ((select auth.uid()) = id);

-- Xóa policy cũ để policy rộng như WITH CHECK (true) không còn tác dụng OR.
do $$
declare
    target_table text;
    policy_record record;
begin
    foreach target_table in array array[
        'projects', 'topics', 'documents', 'summaries', 'flashcards',
        'quizzes', 'notes', 'study_plans', 'reminders', 'chat_history'
    ]
    loop
        execute format('alter table public.%I enable row level security', target_table);
        execute format('revoke all on table public.%I from anon', target_table);
        execute format(
            'grant select, insert, update, delete on table public.%I to authenticated',
            target_table
        );

        for policy_record in
            select policyname
            from pg_policies
            where schemaname = 'public' and tablename = target_table
        loop
            execute format(
                'drop policy %I on public.%I',
                policy_record.policyname,
                target_table
            );
        end loop;

        execute format(
            'create policy %I on public.%I for select to authenticated using ((select auth.uid()) = user_id)',
            target_table || '_select_own',
            target_table
        );
        execute format(
            'create policy %I on public.%I for insert to authenticated with check ((select auth.uid()) = user_id)',
            target_table || '_insert_own',
            target_table
        );
        execute format(
            'create policy %I on public.%I for update to authenticated using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id)',
            target_table || '_update_own',
            target_table
        );
        execute format(
            'create policy %I on public.%I for delete to authenticated using ((select auth.uid()) = user_id)',
            target_table || '_delete_own',
            target_table
        );
    end loop;
end;
$$;

-- Bucket được tạo bằng Dashboard/API; SQL chỉ xác nhận nó tồn tại và đang private.
do $$
begin
    if not exists (
        select 1 from storage.buckets where id = 'documents'
    ) then
        raise exception 'Create the documents bucket before running this migration';
    end if;

    if exists (
        select 1
        from storage.buckets
        where id = 'documents' and public = true
    ) then
        raise exception 'The documents bucket must be private';
    end if;
end;
$$;

-- Xóa hai policy demo rộng và policy chuẩn cũ nếu đã tồn tại.
drop policy if exists "authenticated users can upload documents" on storage.objects;
drop policy if exists "authenticated users can read documents" on storage.objects;
drop policy if exists documents_select_own on storage.objects;
drop policy if exists documents_insert_own on storage.objects;
drop policy if exists documents_update_own on storage.objects;
drop policy if exists documents_delete_own on storage.objects;

create policy documents_select_own
on storage.objects for select to authenticated
using (
    bucket_id = 'documents'
    and (storage.foldername(name))[1] = (select auth.uid())::text
);

create policy documents_insert_own
on storage.objects for insert to authenticated
with check (
    bucket_id = 'documents'
    and (storage.foldername(name))[1] = (select auth.uid())::text
);

create policy documents_update_own
on storage.objects for update to authenticated
using (
    bucket_id = 'documents'
    and (storage.foldername(name))[1] = (select auth.uid())::text
)
with check (
    bucket_id = 'documents'
    and (storage.foldername(name))[1] = (select auth.uid())::text
);

create policy documents_delete_own
on storage.objects for delete to authenticated
using (
    bucket_id = 'documents'
    and (storage.foldername(name))[1] = (select auth.uid())::text
);

commit;
