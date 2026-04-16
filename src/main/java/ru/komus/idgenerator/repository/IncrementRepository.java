package ru.komus.idgenerator.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import ru.komus.idgenerator.data.model.IdIncrement;

import java.util.Optional;

/**
 * This repository manipulates IdIncrement model.
 */
public interface IncrementRepository extends JpaRepository<IdIncrement, Long>
{

    /**
     * Increase increment by its batch size
     * This method usually works with method findByGeneratorCode
     *
     * @param generatorCode code of {@link IdIncrement}
     */
    @Modifying
    @Query(value = "UPDATE IdIncrement i set i.increment = i.increment + i.batch WHERE i.generatorCode = :generatorCode")
    void increaseIncrement(String generatorCode);

    /**
     * Get entity by generatorCode
     *
     * @param generatorCode code of {@link IdIncrement}
     * @return IdIncrement
     */
    Optional<IdIncrement> findByGeneratorCode(String generatorCode);

    /**
     * Delete increment by generatorCode
     *
     * @param generatorCode code of {@link IdIncrement}
     */
    void deleteByGeneratorCode(String generatorCode);

    /**
     * Check if increment exists by generatorCode
     *
     * @param generatorCode code of {@link IdIncrement}
     */
    boolean existsByGeneratorCode(String generatorCode);
}
