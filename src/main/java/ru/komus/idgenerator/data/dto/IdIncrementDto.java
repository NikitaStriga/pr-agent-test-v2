package ru.komus.idgenerator.data.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.komus.idgenerator.data.model.IdIncrement;

import java.util.Objects;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

/**
 * Dto for model IdIncrement to show all values.
 */
@Getter
@Setter
@NoArgsConstructor
public class IdIncrementDto
{

    @NotBlank(message = "generatorCode can't be empty")
    private String generatorCode;

    @Min(value = 0L, message = "The value must be positive")
    private Integer batch;

    @Min(value = 0L, message = "The value must be positive")
    private Long increment;

    /**
     * Constructor copy for IdIncrement
     *
     * @param entity {@link IdIncrement}
     */
    public IdIncrementDto(IdIncrement entity)
    {
        this(entity.getGeneratorCode(), entity.getBatch(), entity.getIncrement());
    }

    public IdIncrementDto(String generatorCode, Integer batch, Long increment)
    {
        this.generatorCode = generatorCode;
        this.batch = batch;
        this.increment = increment;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        IdIncrementDto that = (IdIncrementDto) o;
        return Objects.equals(generatorCode, that.generatorCode);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(generatorCode);
    }
}
