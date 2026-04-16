package ru.komus.idgenerator.data.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * IdIncrement.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IdIncrement
{
    @Id
    private String generatorCode;

    private int batch;

    @Column(name = "incrementValue")
    private Long increment;

    public IdIncrement(IdIncrement increment)
    {
        this.generatorCode = increment.generatorCode;
        this.batch = increment.batch;
        this.increment = increment.increment;
    }
}
