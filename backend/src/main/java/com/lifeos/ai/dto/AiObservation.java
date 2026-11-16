package com.lifeos.ai.dto;

import java.util.List;

public record AiObservation(String title, String detail, String trend, List<AiSourceReference> sourceRefs) { }
