ALTER TABLE ID_INCREMENT DROP COLUMN ID;

declare
constraint_name varchar(20);
select_constraint_name varchar(200);
BEGIN
    select_constraint_name := 'select CONSTRAINT_NAME
    from USER_CONS_COLUMNS
    where TABLE_NAME = ''ID_INCREMENT''
      and COLUMN_NAME = ''GENERATOR_CODE''
      and POSITION = 1';
execute immediate select_constraint_name into constraint_name;
EXECUTE IMMEDIATE 'ALTER TABLE ID_INCREMENT DROP CONSTRAINT ' || constraint_name;
END;