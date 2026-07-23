-- Quiz results table for Personalized Dashboard latest score.

begin;

create extension if not exists pgcrypto with schema extensions;

create table if not exists public.quiz_results (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references public.users(id) on delete cascade,
    quiz_id uuid references public.quizzes(id) on delete set null,
    document_id uuid references public.documents(id) on delete cascade,
    project_id uuid references public.projects(id) on delete set null,
    score integer not null default 0 check (score >= 0),
    total_questions integer not null default 0 check (total_questions >= 0),
    correct_count integer not null default 0 check (correct_count >= 0),
    wrong_count integer not null default 0 check (wrong_count >= 0),
    completed_at timestamptz not null default now()
);

alter table public.quiz_results
    add column if not exists quiz_id uuid references public.quizzes(id) on delete set null,
    add column if not exists project_id uuid references public.projects(id) on delete set null,
    add column if not exists score integer not null default 0 check (score >= 0),
    add column if not exists correct_count integer not null default 0 check (correct_count >= 0),
    add column if not exists wrong_count integer not null default 0 check (wrong_count >= 0),
    add column if not exists completed_at timestamptz not null default now();

do $$
begin
    if exists (
        select 1 from information_schema.columns
        where table_schema = 'public'
          and table_name = 'quiz_results'
          and column_name = 'correct_answers'
    ) then
        update public.quiz_results
        set correct_count = correct_answers
        where correct_count = 0 and correct_answers is not null;
    end if;

    if exists (
        select 1 from information_schema.columns
        where table_schema = 'public'
          and table_name = 'quiz_results'
          and column_name = 'created_at'
    ) then
        update public.quiz_results
        set completed_at = created_at
        where completed_at is null;
    end if;
end;
$$;

update public.quiz_results
set
    wrong_count = greatest(total_questions - correct_count, 0),
    score = correct_count;

create index if not exists idx_quiz_results_user_id on public.quiz_results(user_id);
create index if not exists idx_quiz_results_completed_at on public.quiz_results(completed_at desc);
create index if not exists idx_quiz_results_document_id on public.quiz_results(document_id);

alter table public.quiz_results enable row level security;
revoke all on table public.quiz_results from anon;
grant select, insert, update, delete on table public.quiz_results to authenticated;

drop policy if exists quiz_results_select_own on public.quiz_results;
drop policy if exists quiz_results_insert_own on public.quiz_results;
drop policy if exists quiz_results_update_own on public.quiz_results;
drop policy if exists quiz_results_delete_own on public.quiz_results;

create policy quiz_results_select_own
on public.quiz_results for select to authenticated
using ((select auth.uid()) = user_id);

create policy quiz_results_insert_own
on public.quiz_results for insert to authenticated
with check ((select auth.uid()) = user_id);

create policy quiz_results_update_own
on public.quiz_results for update to authenticated
using ((select auth.uid()) = user_id)
with check ((select auth.uid()) = user_id);

create policy quiz_results_delete_own
on public.quiz_results for delete to authenticated
using ((select auth.uid()) = user_id);

commit;
