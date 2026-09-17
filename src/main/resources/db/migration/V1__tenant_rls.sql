create table permits (
  id uuid primary key,
  tenant_id uuid not null,
  reference varchar(64) not null,
  applicant_name varchar(160) not null,
  status varchar(16) not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (tenant_id, reference)
);
create index permits_tenant_id_idx on permits (tenant_id);

create table audit_events (
  id uuid primary key,
  tenant_id uuid not null,
  actor_subject varchar(255) not null,
  action varchar(80) not null,
  resource_id uuid not null,
  occurred_at timestamptz not null default now()
);
create index audit_events_tenant_resource_idx on audit_events (tenant_id, resource_id);

-- FORCE makes even the table owner subject to the policy. No application query accepts a tenant id.
alter table permits enable row level security;
alter table permits force row level security;
create policy permit_tenant_isolation on permits
  using (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid)
  with check (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid);

alter table audit_events enable row level security;
alter table audit_events force row level security;
create policy audit_tenant_isolation on audit_events
  using (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid)
  with check (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid);
