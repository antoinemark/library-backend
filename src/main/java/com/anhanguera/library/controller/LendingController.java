package com.anhanguera.library.controller;

import com.anhanguera.library.dtos.LendingDTO.LendingRequest;
import com.anhanguera.library.dtos.LendingDTO.LendingResponse;
import com.anhanguera.library.dtos.LendingDTO.LendingReturnRequest;
import com.anhanguera.library.model.LendingStatus;
import com.anhanguera.library.service.LendingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/lendings")
@RequiredArgsConstructor
@Tag(name = "Empréstimos", description = "Registro de empréstimos e devoluções")
public class LendingController {

    private final LendingService lendingService;

    @GetMapping
    @Operation(summary = "Lista os empréstimos", description = "Mais recentes primeiro. Pode filtrar por usuário e por status.")
    public ResponseEntity<List<LendingResponse>> getLendings(
            @Parameter(description = "Id do usuário") @RequestParam(required = false) Long userId,
            @Parameter(description = "Status do empréstimo") @RequestParam(required = false) LendingStatus status) {
        return ResponseEntity.ok(lendingService.getLendings(userId, status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um empréstimo pelo id")
    public ResponseEntity<LendingResponse> getLendingById(@PathVariable Long id) {
        return ResponseEntity.ok(lendingService.getLendingById(id));
    }

    @PostMapping
    @Operation(summary = "Registra um empréstimo",
            description = "Bloqueado (409) se o usuário tiver empréstimos em atraso ou se o livro já estiver emprestado. "
                    + "Datas (400): o empréstimo não pode ser futuro nem anterior à publicação ou à última "
                    + "devolução do livro, e a devolução prevista não pode ser anterior a hoje.")
    public ResponseEntity<LendingResponse> createLending(@Valid @RequestBody LendingRequest request) {
        LendingResponse response = lendingService.createLending(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PatchMapping("/{id}/return")
    @Operation(summary = "Registra a devolução do livro",
            description = "Corpo opcional; sem returnDate, a devolução é registrada com a data de hoje.")
    public ResponseEntity<LendingResponse> returnLending(@PathVariable Long id,
                                                         @RequestBody(required = false) LendingReturnRequest request) {
        return ResponseEntity.ok(lendingService.returnLending(id, request));
    }
}
