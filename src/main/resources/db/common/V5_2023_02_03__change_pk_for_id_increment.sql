DECLARE
    v_column_exists        number := 0;
    constraint_name        varchar(20);
    select_constraint_name varchar(200);
BEGIN
    Select count(*)
    into v_column_exists
    from user_tab_cols
    where upper(column_name) = 'ID'
      and upper(table_name) = 'ID_INCREMENT';
    --  is id exist in ID_INCREMENT table (this check is needed because this script perform in dev scripts earlier)
    if (v_column_exists != 0) then
        execute immediate 'ALTER TABLE ID_INCREMENT DROP COLUMN ID';
        select_constraint_name := 'select CONSTRAINT_NAME
    from USER_CONS_COLUMNS
    where TABLE_NAME = ''ID_INCREMENT''
      and COLUMN_NAME = ''GENERATOR_CODE''
      and POSITION = 1';
        -- find GENERATOR_CODE constraint
        execute immediate select_constraint_name into constraint_name;
        -- delete GENERATOR_CODE constraint
        execute immediate 'ALTER TABLE ID_INCREMENT DROP CONSTRAINT ' || constraint_name;
    end if;
end;