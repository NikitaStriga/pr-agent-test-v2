package ru.komus.idgenerator.data.mappers;


import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ru.komus.idgenerator.data.dto.IdIncrementUpdateDto;
import ru.komus.idgenerator.data.model.IdIncrement;

/**
 * Instructions for mapper that converts dto to entity.
 * It will be generated in target/generated-sources/annotations/ru/komus/idgenerator/data/mappers/
 */
@Mapper(componentModel = "spring")
public interface IdIncrementMapper
{
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateCustomerFromDto(@MappingTarget IdIncrement entity, IdIncrementUpdateDto dto);
}
