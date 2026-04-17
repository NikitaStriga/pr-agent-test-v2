package ru.komus.idgenerator.resources;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.komus.idgenerator.data.dto.IdIncrementDto;
import ru.komus.idgenerator.data.dto.RangeDto;
import ru.komus.idgenerator.service.IncrementService;

import java.util.List;


@RestController
@RequestMapping("/api/v1")
@Tag(name = "Генератор идентификаторов")
public class IncrementController
{
    private final Logger logger = LoggerFactory.getLogger(IncrementController.class);

    private final IncrementService incrementService;

    public IncrementController(IncrementService incrementService)
    {
        this.incrementService = incrementService;
    }

    @PostMapping("/id/{generatorCode}")
    @Operation(description = "Выдать серию идентификаторов")
    public ResponseEntity<RangeDto> getId(@PathVariable String generatorCode)
    {
        RangeDto nextIds = incrementService.getNextIds(generatorCode);
        logger.info("key:{};value:{},{}", generatorCode, nextIds.getStart(), nextIds.getCount());
        return ResponseEntity.ok(incrementService.getNextIds(generatorCode));
    }

    @GetMapping("/ids")
    @Operation(description = "Выдать все идентификаторы и их значение")
    public ResponseEntity<List<IdIncrementDto>> getAllIds()
    {
        List<IdIncrementDto> allIds = incrementService.getAll();
        logger.info("output: {}", allIds);

        return ResponseEntity.ok(incrementService.getAll());
    }

}




