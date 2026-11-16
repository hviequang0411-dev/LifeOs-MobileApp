package com.lifeos.ai.dto;

import java.time.LocalDate;

public record AiTaskDraft(String title, String description, String priority,
        LocalDate suggestedDueDate, String reason) { }
