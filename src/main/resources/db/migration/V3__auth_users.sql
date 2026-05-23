create table rg_user_account (
    user_id varchar(40) not null,
    full_name varchar(120) not null,
    email varchar(190) not null,
    password_hash varchar(100) not null,
    role varchar(40) not null,
    email_verified boolean not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint pk_rg_user primary key (user_id),
    constraint uq_rg_user_email unique (email)
);

create index idx_rg_user_email on rg_user_account (email);

create table rg_email_verification (
    token varchar(90) not null,
    user_id varchar(40) not null,
    expires_at timestamp not null,
    used boolean not null,
    created_at timestamp not null,
    constraint pk_rg_email_verification primary key (token),
    constraint fk_rg_email_verification_user foreign key (user_id) references rg_user_account (user_id)
);

create index idx_rg_email_verification_user on rg_email_verification (user_id, used);

create table rg_password_reset (
    token varchar(90) not null,
    user_id varchar(40) not null,
    expires_at timestamp not null,
    used boolean not null,
    created_at timestamp not null,
    constraint pk_rg_password_reset primary key (token),
    constraint fk_rg_password_reset_user foreign key (user_id) references rg_user_account (user_id)
);

create index idx_rg_password_reset_user on rg_password_reset (user_id, used);
