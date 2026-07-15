package com.timetable.timetable.domain.schedule.dto;

/** dictates if there can be phantom teachers or not */
public enum PhantomTeacherPolicy {
  NEVER, // Falha se não houver professor
  CREATE_IF_NEEDED // Cria fantasma se necessário
}
