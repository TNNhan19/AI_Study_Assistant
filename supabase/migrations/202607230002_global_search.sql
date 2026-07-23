-- Gom global search thành một RPC, có xếp hạng và giới hạn theo loại kết quả.
begin;

create schema if not exists extensions;
create extension if not exists pg_trgm with schema extensions;
set local search_path = public, extensions;

create index if not exists idx_documents_name_trgm
    on public.documents using gin (name gin_trgm_ops);
create index if not exists idx_projects_name_trgm
    on public.projects using gin (name gin_trgm_ops);
create index if not exists idx_projects_description_trgm
    on public.projects using gin (description gin_trgm_ops);
create index if not exists idx_topics_name_trgm
    on public.topics using gin (name gin_trgm_ops);
create index if not exists idx_topics_description_trgm
    on public.topics using gin (description gin_trgm_ops);
create index if not exists idx_notes_title_trgm
    on public.notes using gin (title gin_trgm_ops);
create index if not exists idx_notes_content_trgm
    on public.notes using gin (content gin_trgm_ops);
create index if not exists idx_flashcards_front_trgm
    on public.flashcards using gin (front gin_trgm_ops);
create index if not exists idx_flashcards_back_trgm
    on public.flashcards using gin (back gin_trgm_ops);
create index if not exists idx_quizzes_question_trgm
    on public.quizzes using gin (question gin_trgm_ops);
create index if not exists idx_quizzes_explanation_trgm
    on public.quizzes using gin (explanation gin_trgm_ops);
create index if not exists idx_summaries_text_trgm
    on public.summaries using gin (summary_text gin_trgm_ops);

create or replace function public.global_search(
    p_query text,
    p_limit_per_type integer default 5
)
returns table (
    result_id uuid,
    result_title text,
    result_subtitle text,
    result_type text,
    result_score real
)
language sql
stable
security invoker
set search_path = public, extensions
as $$
with search_input as (
    select trim(coalesce(p_query, '')) as term
),
candidates as (
    select
        d.id as result_id,
        d.name as result_title,
        upper(d.file_type) as result_subtitle,
        'DOCUMENT'::text as result_type,
        (
            case
                when lower(d.name) = lower(q.term) then 100
                when d.name ilike q.term || '%' then 85
                when d.name ilike '%' || q.term || '%' then 70
                else 40
            end
            + similarity(d.name, q.term) * 20
        )::real as result_score
    from public.documents d
    cross join search_input q
    where d.user_id = auth.uid()
      and length(q.term) >= 2
      and (
          d.name ilike '%' || q.term || '%'
          or d.name % q.term
      )

    union all

    select
        p.id,
        p.name,
        coalesce(p.description, ''),
        'PROJECT',
        (
            case
                when lower(p.name) = lower(q.term) then 100
                when p.name ilike q.term || '%' then 85
                when p.name ilike '%' || q.term || '%' then 70
                when coalesce(p.description, '') ilike '%' || q.term || '%' then 55
                else 40
            end
            + greatest(
                similarity(p.name, q.term),
                similarity(coalesce(p.description, ''), q.term)
            ) * 20
        )::real
    from public.projects p
    cross join search_input q
    where p.user_id = auth.uid()
      and length(q.term) >= 2
      and (
          p.name ilike '%' || q.term || '%'
          or coalesce(p.description, '') ilike '%' || q.term || '%'
          or p.name % q.term
          or coalesce(p.description, '') % q.term
      )

    union all

    select
        t.id,
        t.name,
        coalesce(t.description, ''),
        'TOPIC',
        (
            case
                when lower(t.name) = lower(q.term) then 100
                when t.name ilike q.term || '%' then 85
                when t.name ilike '%' || q.term || '%' then 70
                when coalesce(t.description, '') ilike '%' || q.term || '%' then 55
                else 40
            end
            + greatest(
                similarity(t.name, q.term),
                similarity(coalesce(t.description, ''), q.term)
            ) * 20
        )::real
    from public.topics t
    cross join search_input q
    where t.user_id = auth.uid()
      and length(q.term) >= 2
      and (
          t.name ilike '%' || q.term || '%'
          or coalesce(t.description, '') ilike '%' || q.term || '%'
          or t.name % q.term
          or coalesce(t.description, '') % q.term
      )

    union all

    select
        n.id,
        n.title,
        left(coalesce(n.content, ''), 240),
        'NOTE',
        (
            case
                when lower(n.title) = lower(q.term) then 100
                when n.title ilike q.term || '%' then 85
                when n.title ilike '%' || q.term || '%' then 70
                when coalesce(n.content, '') ilike '%' || q.term || '%' then 55
                else 40
            end
            + greatest(
                similarity(n.title, q.term),
                similarity(coalesce(n.content, ''), q.term)
            ) * 20
        )::real
    from public.notes n
    cross join search_input q
    where n.user_id = auth.uid()
      and length(q.term) >= 2
      and (
          n.title ilike '%' || q.term || '%'
          or coalesce(n.content, '') ilike '%' || q.term || '%'
          or n.title % q.term
          or coalesce(n.content, '') % q.term
      )

    union all

    select
        f.document_id,
        f.front,
        left(f.back, 240),
        'FLASHCARD',
        (
            case
                when lower(f.front) = lower(q.term) then 100
                when f.front ilike q.term || '%' then 85
                when f.front ilike '%' || q.term || '%' then 70
                when f.back ilike '%' || q.term || '%' then 55
                else 40
            end
            + greatest(
                similarity(f.front, q.term),
                similarity(f.back, q.term)
            ) * 20
        )::real
    from public.flashcards f
    cross join search_input q
    where f.user_id = auth.uid()
      and length(q.term) >= 2
      and (
          f.front ilike '%' || q.term || '%'
          or f.back ilike '%' || q.term || '%'
          or f.front % q.term
          or f.back % q.term
      )

    union all

    select
        z.document_id,
        z.question,
        left(coalesce(z.explanation, ''), 240),
        'QUIZ',
        (
            case
                when lower(z.question) = lower(q.term) then 100
                when z.question ilike q.term || '%' then 85
                when z.question ilike '%' || q.term || '%' then 70
                when coalesce(z.explanation, '') ilike '%' || q.term || '%' then 55
                else 40
            end
            + greatest(
                similarity(z.question, q.term),
                similarity(coalesce(z.explanation, ''), q.term)
            ) * 20
        )::real
    from public.quizzes z
    cross join search_input q
    where z.user_id = auth.uid()
      and length(q.term) >= 2
      and (
          z.question ilike '%' || q.term || '%'
          or coalesce(z.explanation, '') ilike '%' || q.term || '%'
          or z.question % q.term
          or coalesce(z.explanation, '') % q.term
      )

    union all

    select
        s.document_id,
        d.name,
        left(s.summary_text, 240),
        'DOCUMENT',
        (
            case
                when s.summary_text ilike '%' || q.term || '%' then 60
                else 40
            end
            + similarity(s.summary_text, q.term) * 20
        )::real
    from public.summaries s
    join public.documents d on d.id = s.document_id
    cross join search_input q
    where s.user_id = auth.uid()
      and length(q.term) >= 2
      and (
          s.summary_text ilike '%' || q.term || '%'
          or s.summary_text % q.term
      )
),
ranked as (
    select
        candidates.*,
        row_number() over (
            partition by result_type
            order by result_score desc, result_title
        ) as type_rank
    from candidates
)
select
    result_id,
    result_title,
    result_subtitle,
    result_type,
    result_score
from ranked
where type_rank <= greatest(1, least(coalesce(p_limit_per_type, 5), 20))
order by result_score desc, result_type, result_title;
$$;

revoke all on function public.global_search(text, integer) from public;
grant execute on function public.global_search(text, integer) to authenticated;

notify pgrst, 'reload schema';

commit;
