package ru.komus.idgenerator.data.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * Dto for IdIncrement model to show range of identifiers.
 */
@Getter
@Setter
@Data
public class RangeDto
{

    /**
     * The last value of increment, the first value of the range of indicators
     */
    private Long start;
    /**
     * The batch value of increment
     */
    private int count;
}
