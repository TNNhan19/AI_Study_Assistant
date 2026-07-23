begin;

create table if not exists public.quiz_sets (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references public.users(id) on delete cascade,
    document_id uuid not null references public.documents(id) on delete cascade,
    title text not null check (length(trim(title)) > 0),
    is_pinned boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (user_id, document_id)
);

create index if not exists idx_quiz_sets_user_pinned_updated
    on public.quiz_sets(user_id, is_pinned desc, updated_at desc);

drop trigger if exists set_quiz_sets_updated_at on public.quiz_sets;
create trigger set_quiz_sets_updated_at
before update on public.quiz_sets
for each row execute function public.set_updated_at();

-- Preserve every existing quiz question and create one library entry per document.
insert into public.quiz_sets (
    user_id,
    document_id,
    title,
    created_at,
    updated_at
)
select
    q.user_id,
    q.document_id,
    d.name,
    min(q.created_at),
    max(q.created_at)
from public.quizzes q
join public.documents d on d.id = q.document_id
group by q.user_id, q.document_id, d.name
on conflict (user_id, document_id) do nothing;

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

-- Delete the metadata and every question in the selected set atomically.
create or replace function public.delete_quiz_set(p_quiz_set_id uuid)
returns boolean
language plpgsql
security invoker
set search_path = public
as $$
declare
    target_document_id uuid;
begin
    select document_id
    into target_document_id
    from public.quiz_sets
    where id = p_quiz_set_id
      and user_id = auth.uid();

    if target_document_id is null then
        return false;
    end if;

    delete from public.quizzes
    where user_id = auth.uid()
      and document_id = target_document_id;

    delete from public.quiz_sets
    where id = p_quiz_set_id
      and user_id = auth.uid();

    return true;
end;
$$;

revoke all on function public.delete_quiz_set(uuid) from public;
grant execute on function public.delete_quiz_set(uuid) to authenticated;

notify pgrst, 'reload schema';

commit;
