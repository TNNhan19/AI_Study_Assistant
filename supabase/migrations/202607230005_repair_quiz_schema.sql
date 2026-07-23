begin;

-- Self-contained repair for environments where the app was updated before
-- migrations 003/004, or where those migrations were only partially applied.
create table if not exists public.quiz_sets (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references public.users(id) on delete cascade,
    document_id uuid not null references public.documents(id) on delete cascade,
    title text not null check (length(trim(title)) > 0),
    is_pinned boolean not null default false,
    difficulty text not null default 'MEDIUM',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

alter table public.quiz_sets
    add column if not exists difficulty text not null default 'MEDIUM';
alter table public.quizzes
    add column if not exists quiz_set_id uuid;

alter table public.quiz_sets
    drop constraint if exists quiz_sets_user_id_document_id_key;

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conrelid = 'public.quiz_sets'::regclass
          and conname = 'quiz_sets_difficulty_check'
    ) then
        alter table public.quiz_sets
            add constraint quiz_sets_difficulty_check
            check (difficulty in ('EASY', 'MEDIUM', 'HARD'));
    end if;
end;
$$;

-- Ensure every legacy document with questions has a set before backfilling.
insert into public.quiz_sets (user_id, document_id, title, created_at, updated_at)
select
    q.user_id,
    q.document_id,
    d.name,
    min(q.created_at),
    max(q.created_at)
from public.quizzes q
join public.documents d on d.id = q.document_id
where q.quiz_set_id is null
  and not exists (
      select 1
      from public.quiz_sets existing
      where existing.user_id = q.user_id
        and existing.document_id = q.document_id
  )
group by q.user_id, q.document_id, d.name;

update public.quizzes q
set quiz_set_id = (
    select qs.id
    from public.quiz_sets qs
    where qs.user_id = q.user_id
      and qs.document_id = q.document_id
    order by qs.created_at asc, qs.id asc
    limit 1
)
where q.quiz_set_id is null;

do $$
begin
    if exists (select 1 from public.quizzes where quiz_set_id is null) then
        raise exception 'Cannot repair quizzes without a matching quiz set';
    end if;
end;
$$;

alter table public.quizzes
    alter column quiz_set_id set not null;

alter table public.quizzes
    drop constraint if exists quizzes_quiz_set_id_fkey;

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conrelid = 'public.quiz_sets'::regclass
          and conname = 'quiz_sets_id_user_id_key'
    ) then
        alter table public.quiz_sets
            add constraint quiz_sets_id_user_id_key unique (id, user_id);
    end if;

    if not exists (
        select 1
        from pg_constraint
        where conrelid = 'public.quizzes'::regclass
          and conname = 'quizzes_quiz_set_owner_fkey'
    ) then
        alter table public.quizzes
            add constraint quizzes_quiz_set_owner_fkey
            foreign key (quiz_set_id, user_id)
            references public.quiz_sets(id, user_id)
            on delete cascade;
    end if;
end;
$$;

create index if not exists idx_quiz_sets_user_document
    on public.quiz_sets(user_id, document_id);
create index if not exists idx_quiz_sets_user_pinned_updated
    on public.quiz_sets(user_id, is_pinned desc, updated_at desc);
create index if not exists idx_quizzes_quiz_set_id
    on public.quizzes(quiz_set_id);

drop trigger if exists set_quiz_sets_updated_at on public.quiz_sets;
create trigger set_quiz_sets_updated_at
before update on public.quiz_sets
for each row execute function public.set_updated_at();

alter table public.quiz_sets enable row level security;
revoke all on table public.quiz_sets from anon;
grant select, insert, update, delete on table public.quiz_sets to authenticated;

drop policy if exists quiz_sets_select_own on public.quiz_sets;
drop policy if exists quiz_sets_insert_own on public.quiz_sets;
drop policy if exists quiz_sets_update_own on public.quiz_sets;
drop policy if exists quiz_sets_delete_own on public.quiz_sets;

create policy quiz_sets_select_own
on public.quiz_sets for select to authenticated
using ((select auth.uid()) = user_id);

create policy quiz_sets_insert_own
on public.quiz_sets for insert to authenticated
with check ((select auth.uid()) = user_id);

create policy quiz_sets_update_own
on public.quiz_sets for update to authenticated
using ((select auth.uid()) = user_id)
with check ((select auth.uid()) = user_id);

create policy quiz_sets_delete_own
on public.quiz_sets for delete to authenticated
using ((select auth.uid()) = user_id);

create or replace function public.delete_quiz_set(p_quiz_set_id uuid)
returns boolean
language plpgsql
security invoker
set search_path = public
as $$
declare
    deleted_count integer;
begin
    delete from public.quiz_sets
    where id = p_quiz_set_id
      and user_id = auth.uid();

    get diagnostics deleted_count = row_count;
    return deleted_count > 0;
end;
$$;

revoke all on function public.delete_quiz_set(uuid) from public;
grant execute on function public.delete_quiz_set(uuid) to authenticated;

notify pgrst, 'reload schema';

commit;
