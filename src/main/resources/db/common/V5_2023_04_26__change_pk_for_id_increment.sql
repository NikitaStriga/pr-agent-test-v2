DECLARE
    v_constraint_exists        number := 0;
BEGIN
    Select count(*)
    into v_constraint_exists
    FROM user_constraints
    WHERE upper(table_name) = 'ID_INCREMENT'
      AND constraint_type = 'P';
    --  is id exist in ID_INCREMENT table (this check is needed because this script perform in dev scripts earlier)
    if (v_constraint_exists = 0) then
        -- create new pk (generator_code)
        execute immediate 'ALTER TABLE ID_INCREMENT ADD CONSTRAINT pk_generator_code PRIMARY KEY (generator_code)';
    end if;
    commit;
end;