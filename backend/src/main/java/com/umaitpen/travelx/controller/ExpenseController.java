package com.umaitpen.travelx.controller;

import com.umaitpen.travelx.dto.BudgetAnalysisDTO;
import com.umaitpen.travelx.model.Expense;
import com.umaitpen.travelx.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips/{tripId}/expenses")
@RequiredArgsConstructor
public class ExpenseController {
    private final ExpenseService expenseService;

    @PostMapping
    public ResponseEntity<?> addExpense(@PathVariable Long tripId, @RequestBody Expense expense) {
        Expense saved = expenseService.addExpense(tripId, expense);
        if (saved == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(saved);
    }

    @GetMapping
    public ResponseEntity<List<Expense>> getExpenses(@PathVariable Long tripId) {
        return ResponseEntity.ok(expenseService.getExpensesForTrip(tripId));
    }

    @GetMapping("/analysis")
    public ResponseEntity<BudgetAnalysisDTO> analyzeBudget(@PathVariable Long tripId) {
        BudgetAnalysisDTO analysis = expenseService.analyzeBudget(tripId);
        if (analysis == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(analysis);
    }
}
