package com.umaitpen.travelx.dto;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetAnalysisDTO {
    private Double totalBudget;
    private Double estimatedSpend;
    private Double remainingBudget;
    private String status;
    private Map<String, Double> expenseBreakdown;
    private String[] aiInsights;
}
