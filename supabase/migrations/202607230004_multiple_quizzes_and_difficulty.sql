begin;

-- A document may own any number of independently named quiz sets.
alter table public.quiz_sets
    drop constraint if exists quiz_sets_user_id_document_id_key;

alter table public.quiz_sets
    add column if not exists difficulty text not null default 'MEDIUM';

alter table public.quiz_sets
    drop constraint if exists quiz_sets_difficulty_check;
alter table public.quiz_sets
    add constraint quiz_sets_difficulty_check
    check (difficulty in ('EASY', 'MEDIUM', 'HARD'));

-- Questions must belong to a concrete set, not only to their source document.
alter table public.quizzes
    add column if not exists quiz_set_id uuid
    references public.quiz_sets(id) on delete cascade;

update public.quizzes q
set quiz_set_id = qs.id
from public.quiz_sets qs
where q.quiz_set_id is null
  and qs.user_id = q.user_id
  and qs.document_id = q.document_id;

do $$
begin
    if exists (
        select 1
        from public.quizzes
        where quiz_set_id is null
    ) then
        raise exception 'Cannot migrate quizzes without a matching quiz_set';
    end if;
end;
$$;

alter table public.quizzes
    alter column quiz_set_id set not null;

-- Keep the set and every question under the same owner.
alter table public.quizzes
    drop constraint if exists quizzes_quiz_set_id_fkey;
alter table public.quiz_sets
    add constraint quiz_sets_id_user_id_key unique (id, user_id);
alter table public.quizzes
    add constraint quizzes_quiz_set_owner_fkey
    foreign key (quiz_set_id, user_id)
    references public.quiz_sets(id, user_id)
    on delete cascade;

create index if not exists idx_quiz_sets_user_document
    on public.quiz_sets(user_id, document_id);
create index if not exists idx_quizzes_quiz_set_id
    on public.quizzes(quiz_set_id);

-- Deleting one set must never remove sibling quizzes from the same document.
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

notify pgrst, 'reload schema';

commit;
