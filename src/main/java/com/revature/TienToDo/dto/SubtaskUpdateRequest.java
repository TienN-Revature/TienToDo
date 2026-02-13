package com.revature.TienToDo.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubtaskUpdateRequest {
    @Size(max = 255, message = "Title must be at most 255 characters")
    private String title;

    private Boolean completed;
}
