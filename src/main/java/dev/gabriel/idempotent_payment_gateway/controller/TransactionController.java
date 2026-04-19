package dev.gabriel.idempotent_payment_gateway.controller;

import dev.gabriel.idempotent_payment_gateway.model.dtos.TransactionRequestDto;
import dev.gabriel.idempotent_payment_gateway.model.dtos.TransactionResponseDto;
import dev.gabriel.idempotent_payment_gateway.service.IdempotencyScope;
import dev.gabriel.idempotent_payment_gateway.service.IdempotencyService;
import dev.gabriel.idempotent_payment_gateway.service.IdempotentPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Idempotent payment API (distributed locks + Redis idempotency cache)")
public class TransactionController {

    private final IdempotencyService idempotencyService;
    private final IdempotentPaymentService idempotentPaymentService;

    @PostMapping
    @Operation(summary = "Process a transaction", description = "Debits or credits an account. Idempotency-Key + Redis + distributed lock; DB pessimistic lock on the account row for consistency under 4K+ style concurrency.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "transaction processed successfully"),
            @ApiResponse(responseCode = "200", description = "transaction returned from cache (idempotent)")
    })
    public ResponseEntity<TransactionResponseDto> createTransaction(
            @Parameter(description = "unique key to ensure idempotency", required = true)
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody @Valid TransactionRequestDto requestDto
    ) {
        TransactionResponseDto cachedResponse = idempotencyService.get(IdempotencyScope.PAYMENT_API, idempotencyKey);
        if (cachedResponse != null) {
            return ResponseEntity.ok(cachedResponse);
        }

        TransactionResponseDto responseDto = idempotentPaymentService.processPayment(idempotencyKey, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

}