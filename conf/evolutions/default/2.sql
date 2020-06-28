# --- !Ups
alter table box add inclusion_height integer;


# --- !Downs
create table box_tmp
(
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

insert into box_tmp(id, value, creation_height, box_index, address, ergo_tree, transaction_id, spent_in_id, created_in_id, bytes) select id, value, creation_height, box_index, address, ergo_tree, transaction_id, spent_in_id, created_in_id, bytes from box;

drop table box;

alter table box_tmp rename to box;