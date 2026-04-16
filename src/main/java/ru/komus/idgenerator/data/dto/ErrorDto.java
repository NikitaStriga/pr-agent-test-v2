
package ru.komus.idgenerator.data.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * Dto for error response.
 */
@Getter
@Setter
@AllArgsConstructor
@Data
public class ErrorDto
{
    private String errorMessage;
    private String path;
}
