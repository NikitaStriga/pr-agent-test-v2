package ru.komus.idgenerator.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import ru.komus.idgenerator.data.dto.IdIncrementDto;
import ru.komus.idgenerator.data.dto.IdIncrementUpdateDto;
import ru.komus.idgenerator.data.dto.RangeDto;
import ru.komus.idgenerator.data.exceptions.AlreadyExistsException;
import ru.komus.idgenerator.data.mappers.IdIncrementMapper;
import ru.komus.idgenerator.data.model.IdIncrement;
import ru.komus.idgenerator.repository.IncrementRepository;
import ru.komus.idgenerator.resources.AdminController;
import ru.komus.idgenerator.service.IncrementService;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Main implementation of IncrementService.
 */
@Service
public class IncrementServiceImpl implements IncrementService
{

    private final Logger logger = LoggerFactory.getLogger(AdminController.class);


    private final IncrementRepository incrementRepository;
    private final IdIncrementMapper idIncrementMapper;

    public IncrementServiceImpl(final IncrementRepository incrementRepository, IdIncrementMapper idIncrementMapper)
    {
        this.incrementRepository = incrementRepository;
        this.idIncrementMapper = idIncrementMapper;
    }

    /**
     * Returns next value of increment by generatorCode.
     *
     * @param generatorCode code of {@link IdIncrement}
     * @return RangeDto {@link RangeDto}
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Override
    public RangeDto getNextIds(final String generatorCode)
    {
        incrementRepository.increaseIncrement(generatorCode);
        IdIncrement idIncrement = findIdIncrementByCode(generatorCode);
        RangeDto rangeDto = new RangeDto();
        rangeDto.setStart(idIncrement.getIncrement() - idIncrement.getBatch());
        rangeDto.setCount(idIncrement.getBatch());
        return rangeDto;
    }

    /**
     * Return all increments.
     *
     * @return List of {@link IdIncrementDto}
     */
    @Transactional(readOnly = true)
    @Override
    public List<IdIncrementDto> getAll()
    {
        List<IdIncrement> allIncrements = incrementRepository.findAll();

        return allIncrements.stream().map(IdIncrementDto::new)
            .collect(Collectors.toList());
    }

    /**
     * Create new increment.
     *
     * @param incrementDto {@link IdIncrementDto}
     * @return IdIncrementDto {@link IdIncrementDto}
     */
    @Transactional
    @Override
    public IdIncrementDto create(IdIncrementDto incrementDto)
    {
        if (incrementRepository.existsByGeneratorCode(incrementDto.getGeneratorCode()))
        {
            throw new AlreadyExistsException(incrementDto.getGeneratorCode() + " already exists.");
        }
        IdIncrement increment = new IdIncrement(incrementDto.getGeneratorCode(), incrementDto.getBatch(), incrementDto.getIncrement());
        incrementRepository.save(increment);
        logger.info("new increment {} was created", increment);
        return new IdIncrementDto(increment);
    }

    /**
     * Update increment.
     *
     * @param generatorCode code of {@link IdIncrement}
     * @param incrementDto {@link IdIncrementDto}
     * @return updated values of IdIncrement in the form of IdIncrementDto {@link IdIncrementDto}
     */
    @Transactional
    @Override
    public IdIncrementDto update(String generatorCode, IdIncrementUpdateDto incrementDto)
    {
        IdIncrement idIncrement = findIdIncrementByCode(generatorCode);
        idIncrementMapper.updateCustomerFromDto(idIncrement, incrementDto);
        incrementRepository.save(idIncrement);
        logger.info("increment {} was updated to {}", generatorCode, idIncrement);
        return new IdIncrementDto(idIncrement);
    }

    /**
     * Delete increment.
     *
     * @param generatorCode code of {@link IdIncrement}
     */
    @Transactional
    @Override
    public void delete(String generatorCode)
    {
        findIdIncrementByCode(generatorCode);
        incrementRepository.deleteByGeneratorCode(generatorCode);
        logger.info("increment {} was deleted", generatorCode);

    }

    /**
     * Get entity by generatorCode otherwise throws a {@link NoSuchElementException}
     *
     * @param generatorCode code of {@link IdIncrement}
     * @return IdIncrement
     */
    private IdIncrement findIdIncrementByCode(String generatorCode)
    {
        Optional<IdIncrement> idIncrementOpt = incrementRepository.findByGeneratorCode(generatorCode);
        if (idIncrementOpt.isEmpty())
        {
            throw new NoSuchElementException("generatorCode " + generatorCode + " does not exist.");
        }
        return idIncrementOpt.get();
    }
}
