package ru.komus.idgenerator.data.dto;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.komus.idgenerator.data.model.IdIncrement;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

/**
 * Dto for model IdIncrementUpdateDto to update IdIncrement.
 */
@Getter
@Setter
@NoArgsConstructor
@Data
public class IdIncrementUpdateDto
{

    private Integer batch;

    private Long increment;


    public IdIncrementUpdateDto(Integer batch, Long increment)
    {
        this.batch = batch;
        this.increment = increment;
    }
}
