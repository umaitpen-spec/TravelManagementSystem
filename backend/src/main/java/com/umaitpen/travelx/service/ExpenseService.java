package com.umaitpen.travelx.service;

import com.umaitpen.travelx.dto.BudgetAnalysisDTO;
import com.umaitpen.travelx.model.Expense;

import java.util.List;

public interface ExpenseService {
    Expense addExpense(Long tripId, Expense expense);
    List<Expense> getExpensesForTrip(Long tripId);
    BudgetAnalysisDTO analyzeBudget(Long tripId);
}  
