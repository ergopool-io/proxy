# --- !Ups
create table block (
  id                            integer not null,
  block_id                      varchar(255) not null,
  height                        integer not null,
  constraint uq_block_block_id unique (block_id),
  constraint pk_block primary key (id)
);

create table box (
  id                            varchar(255) not null,
  value                         integer not null,
  creation_height               integer not null,
  box_index                     integer not null,
  address                       varchar(255) not null,
  ergo_tree                     varchar(500) not null,
  transaction_id                varchar(100),
  bytes                         varchar(500),
  spent_in_id                   integer,
  created_in_id                 integer not null,
  constraint pk_box primary key (id),
  foreign key (spent_in_id) references block (id) on delete set null on update set null,
  foreign key (created_in_id) references block (id) on delete cascade on update cascade
);


# --- !Downs

drop table if exists block;

drop table if exists box;

