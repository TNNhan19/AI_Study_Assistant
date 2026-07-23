-- Đồng bộ bảng chat_history cũ với context chat theo tài liệu, chủ đề và dự án.
begin;

alter table public.chat_history
    add column if not exists project_id uuid references public.projects(id) on delete set null,
    add column if not exists topic_id uuid references public.topics(id) on delete set null,
    add column if not exists document_id uuid references public.documents(id) on delete cascade,
    add column if not exists role text,
    add column if not exists content text,
    add column if not exists created_at timestamptz default now();

-- Bảng cũ có thể đã có record nhưng chưa có hai trường bắt buộc.
update public.chat_history
set role = case
    when upper(role) in ('USER', 'ASSISTANT', 'SYSTEM') then upper(role)
    else 'USER'
end;

update public.chat_history
set content = ''
where content is null;

update public.chat_history
set created_at = now()
where created_at is null;

alter table public.chat_history
    alter column role set not null,
    alter column content set not null,
    alter column created_at set default now(),
    alter column created_at set not null;

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conrelid = 'public.chat_history'::regclass
          and conname = 'chat_history_role_check'
    ) then
        alter table public.chat_history
            add constraint chat_history_role_check
            check (role in ('USER', 'ASSISTANT', 'SYSTEM'));
    end if;
end;
$$;

create index if not exists idx_chat_history_document_id
    on public.chat_history(document_id);
create index if not exists idx_chat_history_topic_id
    on public.chat_history(topic_id);
create index if not exists idx_chat_history_project_id
    on public.chat_history(project_id);

grant select, insert, update, delete on public.chat_history to authenticated;

-- Yêu cầu PostgREST cập nhật schema cache ngay sau migration.
notify pgrst, 'reload schema';

commit;
