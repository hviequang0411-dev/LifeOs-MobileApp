package com.lifeos.finance.controller;

import com.lifeos.common.response.PageResponse;
import com.lifeos.common.security.CurrentUser;
import com.lifeos.common.util.Patch;
import com.lifeos.finance.dto.CashFlowResponse;
import com.lifeos.finance.dto.CreateTransactionRequest;
import com.lifeos.finance.dto.CreateTransferRequest;
import com.lifeos.finance.dto.MonthlyReportResponse;
import com.lifeos.finance.dto.TransactionResponse;
import com.lifeos.finance.dto.TransferResponse;
import com.lifeos.finance.dto.UpdateTransactionRequest;
import com.lifeos.finance.dto.UpdateTransferRequest;
import com.lifeos.finance.service.FinanceCommands;
import com.lifeos.finance.service.FinanceQueryService;
import com.lifeos.finance.service.FinanceReportService;
import com.lifeos.finance.service.TransactionCommandService;
import com.lifeos.finance.service.TransferCommandService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Giao dịch, chuyển khoản và báo cáo (PHASE_3 §6). userId luôn lấy từ CurrentUser. */
@RestController
@RequestMapping("/api/v1/finance")
class FinanceTransactionController {

    private final CurrentUser currentUser;
    private final TransactionCommandService transactions;
    private final TransferCommandService transfers;
    private final FinanceQueryService queries;
    private final FinanceReportService reports;

    FinanceTransactionController(CurrentUser currentUser, TransactionCommandService transactions,
            TransferCommandService transfers, FinanceQueryService queries, FinanceReportService reports) {
        this.currentUser = currentUser;
        this.transactions = transactions;
        this.transfers = transfers;
        this.queries = queries;
        this.reports = reports;
    }

    @ApiResponse(responseCode = "201", description = "Đã tạo", content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = TransactionResponse.class)))
    @PostMapping("/transactions")
    ResponseEntity<TransactionResponse> createTransaction(@Valid @RequestBody CreateTransactionRequest r) {
        TransactionResponse created = transactions.create(currentUser.userId(), new FinanceCommands.CreateTransaction(
                r.id(), Patch.of(r.accountId()), Patch.of(r.categoryId()), Patch.of(r.kind()), Patch.of(r.amount()),
                Patch.of(r.occurredOn()), Patch.of(r.note())), null);
        return ResponseEntity.created(URI.create("/api/v1/finance/transactions/" + created.id())).body(created);
    }

    @GetMapping("/transactions")
    PageResponse<TransactionResponse> listTransactions(@RequestParam(required = false) String kind,
            @RequestParam(required = false) UUID accountId, @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String from, @RequestParam(required = false) String to,
            @RequestParam(required = false) String minAmount, @RequestParam(required = false) String maxAmount,
            @RequestParam(required = false) String q, @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor) {
        return queries.transactions(currentUser.userId(), kind, accountId, categoryId, from, to, minAmount, maxAmount,
                q, limit, cursor);
    }

    @GetMapping("/transactions/{id}")
    TransactionResponse getTransaction(@PathVariable UUID id) {
        return queries.transaction(currentUser.userId(), id);
    }

    @PatchMapping("/transactions/{id}")
    TransactionResponse updateTransaction(@PathVariable UUID id, @Valid @RequestBody UpdateTransactionRequest r) {
        return transactions.update(currentUser.userId(), id, new FinanceCommands.UpdateTransaction(r.version(),
                r.accountId(), r.categoryId(), r.kind(), r.amount(), r.occurredOn(), r.note()), null);
    }

    @ApiResponse(responseCode = "204", description = "Đã xóa (tombstone)")
    @DeleteMapping("/transactions/{id}")
    ResponseEntity<Void> deleteTransaction(@PathVariable UUID id, @RequestParam long version) {
        transactions.delete(currentUser.userId(), id, version, null);
        return ResponseEntity.noContent().build();
    }

    @ApiResponse(responseCode = "201", description = "Đã tạo", content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = TransferResponse.class)))
    @PostMapping("/transfers")
    ResponseEntity<TransferResponse> createTransfer(@Valid @RequestBody CreateTransferRequest r) {
        TransferResponse created = transfers.create(currentUser.userId(), new FinanceCommands.CreateTransfer(r.id(),
                Patch.of(r.fromAccountId()), Patch.of(r.toAccountId()), Patch.of(r.amount()), Patch.of(r.occurredOn()),
                Patch.of(r.note())), null);
        return ResponseEntity.created(URI.create("/api/v1/finance/transfers/" + created.id())).body(created);
    }

    @GetMapping("/transfers")
    PageResponse<TransferResponse> listTransfers(@RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) String from, @RequestParam(required = false) String to,
            @RequestParam(required = false) Integer limit, @RequestParam(required = false) String cursor) {
        return queries.transfers(currentUser.userId(), accountId, from, to, limit, cursor);
    }

    @GetMapping("/transfers/{id}")
    TransferResponse getTransfer(@PathVariable UUID id) {
        return queries.transfer(currentUser.userId(), id);
    }

    @PatchMapping("/transfers/{id}")
    TransferResponse updateTransfer(@PathVariable UUID id, @Valid @RequestBody UpdateTransferRequest r) {
        return transfers.update(currentUser.userId(), id, new FinanceCommands.UpdateTransfer(r.version(),
                r.fromAccountId(), r.toAccountId(), r.amount(), r.occurredOn(), r.note()), null);
    }

    @ApiResponse(responseCode = "204", description = "Đã xóa (tombstone)")
    @DeleteMapping("/transfers/{id}")
    ResponseEntity<Void> deleteTransfer(@PathVariable UUID id, @RequestParam long version) {
        transfers.delete(currentUser.userId(), id, version, null);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/reports/monthly")
    MonthlyReportResponse monthly(@RequestParam(required = false) String month, @RequestParam String currency) {
        return reports.monthly(currentUser.userId(), month, currency);
    }

    @GetMapping("/reports/cash-flow")
    CashFlowResponse cashFlow(@RequestParam String from, @RequestParam String to,
            @RequestParam(required = false) String granularity, @RequestParam String currency) {
        return reports.cashFlow(currentUser.userId(), from, to, granularity, currency);
    }
}
