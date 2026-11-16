package com.lifeos.finance.controller;

import com.lifeos.common.response.PageResponse;
import com.lifeos.common.security.CurrentUser;
import com.lifeos.common.util.Patch;
import com.lifeos.common.web.VersionRequest;
import com.lifeos.finance.dto.AccountResponse;
import com.lifeos.finance.dto.AccountView;
import com.lifeos.finance.dto.BalanceResponse;
import com.lifeos.finance.dto.FinanceCategoryResponse;
import com.lifeos.finance.dto.CreateAccountRequest;
import com.lifeos.finance.dto.FinanceCreateCategoryRequest;
import com.lifeos.finance.dto.ReconcileRequest;
import com.lifeos.finance.dto.ReconcileResponse;
import com.lifeos.finance.dto.UpdateAccountRequest;
import com.lifeos.finance.dto.FinanceUpdateCategoryRequest;
import com.lifeos.finance.service.AccountCommandService;
import com.lifeos.finance.service.FinanceCategoryCommandService;
import com.lifeos.finance.service.FinanceCommands;
import com.lifeos.finance.service.FinanceQueryService;
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

/** Tài khoản và danh mục thu/chi của user hiện tại (PHASE_3 §6). userId luôn lấy từ CurrentUser. */
@RestController
@RequestMapping("/api/v1/finance")
class FinanceAccountController {

    private final CurrentUser currentUser;
    private final AccountCommandService accounts;
    private final FinanceCategoryCommandService categories;
    private final FinanceQueryService queries;

    FinanceAccountController(CurrentUser currentUser, AccountCommandService accounts,
            FinanceCategoryCommandService categories, FinanceQueryService queries) {
        this.currentUser = currentUser;
        this.accounts = accounts;
        this.categories = categories;
        this.queries = queries;
    }

    // ---------------------------------------------------------------- tài khoản

    @ApiResponse(responseCode = "201", description = "Đã tạo",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AccountResponse.class)))
    @PostMapping("/accounts")
    ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest r) {
        AccountResponse created = accounts.create(currentUser.userId(), new FinanceCommands.CreateAccount(r.id(),
                Patch.of(r.name()), Patch.of(r.accountType()), Patch.of(r.currency()), Patch.of(r.openingBalance())),
                null);
        return ResponseEntity.created(URI.create("/api/v1/finance/accounts/" + created.id())).body(created);
    }

    @GetMapping("/accounts")
    PageResponse<AccountView> listAccounts(@RequestParam(required = false) String archived,
            @RequestParam(required = false) Integer limit, @RequestParam(required = false) String cursor) {
        return queries.accounts(currentUser.userId(), archived, limit, cursor);
    }

    @GetMapping("/accounts/{id}")
    AccountView getAccount(@PathVariable UUID id) {
        return queries.account(currentUser.userId(), id);
    }

    @GetMapping("/accounts/{id}/balance")
    BalanceResponse balance(@PathVariable UUID id, @RequestParam(required = false) String asOf) {
        return queries.balance(currentUser.userId(), id, asOf);
    }

    @PostMapping("/accounts/{id}/reconcile")
    ReconcileResponse reconcile(@PathVariable UUID id, @Valid @RequestBody ReconcileRequest r) {
        return queries.reconcile(currentUser.userId(), id, r.statementBalance(), r.asOfDate());
    }

    @PatchMapping("/accounts/{id}")
    AccountResponse updateAccount(@PathVariable UUID id, @Valid @RequestBody UpdateAccountRequest r) {
        return accounts.update(currentUser.userId(), id, new FinanceCommands.UpdateAccount(r.version(), r.name(),
                r.accountType(), r.currency(), r.openingBalance(), Patch.absent()), null);
    }

    @PostMapping("/accounts/{id}/archive")
    AccountResponse archiveAccount(@PathVariable UUID id, @Valid @RequestBody VersionRequest r) {
        return accounts.archive(currentUser.userId(), id, r.version());
    }

    @PostMapping("/accounts/{id}/unarchive")
    AccountResponse unarchiveAccount(@PathVariable UUID id, @Valid @RequestBody VersionRequest r) {
        return accounts.unarchive(currentUser.userId(), id, r.version());
    }

    @ApiResponse(responseCode = "204", description = "Đã xóa (tombstone)")
    @DeleteMapping("/accounts/{id}")
    ResponseEntity<Void> deleteAccount(@PathVariable UUID id, @RequestParam long version) {
        accounts.delete(currentUser.userId(), id, version, null);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------- danh mục

    @ApiResponse(responseCode = "201", description = "Đã tạo",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = FinanceCategoryResponse.class)))
    @PostMapping("/categories")
    ResponseEntity<FinanceCategoryResponse> createCategory(@Valid @RequestBody FinanceCreateCategoryRequest r) {
        FinanceCategoryResponse created = categories.create(currentUser.userId(), new FinanceCommands.CreateCategory(r.id(),
                Patch.of(r.name()), Patch.of(r.kind())), null);
        return ResponseEntity.created(URI.create("/api/v1/finance/categories/" + created.id())).body(created);
    }

    @GetMapping("/categories")
    PageResponse<FinanceCategoryResponse> listCategories(@RequestParam(required = false) String kind,
            @RequestParam(required = false) String archived, @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor) {
        return queries.categories(currentUser.userId(), kind, archived, limit, cursor);
    }

    @GetMapping("/categories/{id}")
    FinanceCategoryResponse getCategory(@PathVariable UUID id) {
        return queries.category(currentUser.userId(), id);
    }

    @PatchMapping("/categories/{id}")
    FinanceCategoryResponse updateCategory(@PathVariable UUID id, @Valid @RequestBody FinanceUpdateCategoryRequest r) {
        return categories.update(currentUser.userId(), id, new FinanceCommands.UpdateCategory(r.version(), r.name(),
                r.kind(), Patch.absent()), null);
    }

    @PostMapping("/categories/{id}/archive")
    FinanceCategoryResponse archiveCategory(@PathVariable UUID id, @Valid @RequestBody VersionRequest r) {
        return categories.archive(currentUser.userId(), id, r.version());
    }

    @PostMapping("/categories/{id}/unarchive")
    FinanceCategoryResponse unarchiveCategory(@PathVariable UUID id, @Valid @RequestBody VersionRequest r) {
        return categories.unarchive(currentUser.userId(), id, r.version());
    }

    @ApiResponse(responseCode = "204", description = "Đã xóa (tombstone)")
    @DeleteMapping("/categories/{id}")
    ResponseEntity<Void> deleteCategory(@PathVariable UUID id, @RequestParam long version) {
        categories.delete(currentUser.userId(), id, version, null);
        return ResponseEntity.noContent().build();
    }
}
