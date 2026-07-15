package com.timetable.timetable.domain.schedule.dto;

import java.util.UUID;

public record GenerationStartResult(UUID jobId, PreSolverResult preparationResult) {}
