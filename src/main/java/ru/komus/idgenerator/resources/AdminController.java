package ru.komus.idgenerator.resources;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.komus.idgenerator.data.dto.IdIncrementDto;
import ru.komus.idgenerator.data.dto.IdIncrementUpdateDto;
import ru.komus.idgenerator.service.IncrementService;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/key")
@Tag(name = "Панель администратора")
public class AdminController
{
    private final IncrementService incrementService;

    public AdminController(IncrementService incrementService)
    {
        this.incrementService = incrementService;
    }

    @PostMapping()
    @Operation(description = "Создать новый идентификатор")
    public ResponseEntity<IdIncrementDto> createIncrement(@Valid @RequestBody IdIncrementDto incrementDto)
    {
        return new ResponseEntity(incrementService.create(incrementDto), HttpStatus.CREATED);
    }

    @PatchMapping("/{generatorCode}")
    @Operation(description = "Изменить идентификатор")
    public ResponseEntity<IdIncrementDto> editIncrement(@PathVariable String generatorCode, @RequestBody IdIncrementUpdateDto incrementDto)
    {
        return new ResponseEntity(incrementService.update(generatorCode, incrementDto), HttpStatus.OK);
    }

    @DeleteMapping("/{generatorCode}")
    @Operation(description = "Удалить идентификатор")
    public ResponseEntity deleteIncrement(@PathVariable String generatorCode)
    {
        incrementService.delete(generatorCode);
        return new ResponseEntity(HttpStatus.OK);
    }

}