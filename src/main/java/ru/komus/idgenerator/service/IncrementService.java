package ru.komus.idgenerator.service;

import ru.komus.idgenerator.data.dto.IdIncrementDto;
import ru.komus.idgenerator.data.dto.IdIncrementUpdateDto;
import ru.komus.idgenerator.data.dto.RangeDto;

import java.util.List;

/**
 * This Service manipulates IdIncrement model.
 */
public interface IncrementService
{

    RangeDto getNextIds(String generatorCode);

    List<IdIncrementDto> getAll();

    IdIncrementDto create(IdIncrementDto incrementDto);

    IdIncrementDto update(String generatorCode, IdIncrementUpdateDto incrementDto);

    void delete(String generatorCode);
}
