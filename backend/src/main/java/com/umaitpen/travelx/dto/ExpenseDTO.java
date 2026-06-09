package com.umaitpen.travelx.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseDTO {
    private String category;
    private String description;
    private Double amount;
}
