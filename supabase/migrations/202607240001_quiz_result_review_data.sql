begin;

alter table public.quiz_results
    add column if not exists quiz_set_id uuid
        references public.quiz_sets(id) on delete set null,
    add column if not exists answer_data jsonb;

update public.quiz_results qr
set quiz_set_id = q.quiz_set_id
from public.quizzes q
where qr.quiz_set_id is null
  and qr.quiz_id = q.id;

create index if not exists idx_quiz_results_quiz_set_id
    on public.quiz_results(quiz_set_id);

notify pgrst, 'reload schema';

commit;
