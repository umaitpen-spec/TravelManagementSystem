package com.umaitpen.travelx.service.impl;

import com.umaitpen.travelx.dto.BudgetAnalysisDTO;
import com.umaitpen.travelx.model.Expense;
import com.umaitpen.travelx.model.Trip;
import com.umaitpen.travelx.repository.ExpenseRepository;
import com.umaitpen.travelx.repository.TripRepository;
import com.umaitpen.travelx.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExpenseServiceImpl implements ExpenseService {
    private final ExpenseRepository expenseRepository;
    private final TripRepository tripRepository;

    @Override
    public Expense addExpense(Long tripId, Expense expense) {
        Trip trip = tripRepository.findById(tripId).orElse(null);
        if (trip == null) {
            return null;
        }
        expense.setTrip(trip);
        return expenseRepository.save(expense);
    }

    @Override
    public List<Expense> getExpensesForTrip(Long tripId) {
        return expenseRepository.findByTripId(tripId);
    }

    @Override
    public BudgetAnalysisDTO analyzeBudget(Long tripId) {
        Trip trip = tripRepository.findById(tripId).orElse(null);
        if (trip == null) {
            return null;
        }

        List<Expense> expenses = expenseRepository.findByTripId(tripId);
        double estimatedSpend = expenses.stream().mapToDouble(Expense::getAmount).sum();
        double remainingBudget = trip.getBudget() - estimatedSpend;
        String status = remainingBudget < 0 ? "OVER_BUDGET" : remainingBudget <= trip.getBudget() * 0.15 ? "OPTIMAL" : "UNDER_BUDGET";

        Map<String, Double> breakdown = new HashMap<>();
        expenses.forEach(expense -> breakdown.merge(expense.getCategory(), expense.getAmount(), Double::sum));

        String[] insights = new String[]{
                "Track high-cost travel categories each week.",
                "Review hotel and food expenses to stay within budget.",
                remainingBudget < 0 ? "Warning: your trip is currently over budget." : "Your budget buffer is healthy."
        };

        return BudgetAnalysisDTO.builder()
                .totalBudget(trip.getBudget())
                .estimatedSpend(estimatedSpend)
                .remainingBudget(remainingBudget)
                .status(status)
                .expenseBreakdown(breakdown)
                .aiInsights(insights)
                .build();
    }
}
