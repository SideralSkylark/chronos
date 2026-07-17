# University Timetable Management System

An automated university scheduling platform built with Spring Boot, Vue 3 and Timefold Solver.

The project focuses on two core capabilities:

- Generate feasible academic timetables while respecting institutional constraints.
- Allow administrators to manually adjust scheduled lessons without introducing conflicts.

---

## Technology Stack

### Backend

- Java 21
- Spring Boot 3
- Timefold Solver
- Spring Security (JWT)
- PostgreSQL
- Maven

### Frontend

- Vue 3
- TypeScript
- Pinia
- Tailwind CSS
- Vite

---

## Architecture

### Backend

- `auth/` – authentication and authorization
- `common/` – shared utilities and exception handling
- `config/` – Spring configuration
- `domain/` – JPA entities
- `scheduler_engine/`
    - `domain/`
    - `solver/`
    - `preparation/`

### Frontend

- `components/`
- `views/`
- `stores/`
- `services/`
- `layouts/`
- `composables/`

---

## Scheduler Pipeline

Timetable generation is performed in three stages:

1. Data preparation
2. Lesson initialization
3. Constraint optimization using Timefold Solver

The solver enforces hard constraints (teacher conflicts, room conflicts, etc.) while optimizing soft constraints such as workload balancing.

---

## Testing

The project is currently being hardened for production use.

Testing focuses on proving the two core guarantees of the system.

### Timetable Generation

- [ ] Solver integration tests
- [ ] Constraint regression tests
- [ ] Optional group regression tests

### Manual Adjustments

- [ ] Teacher swap tests
- [ ] Cohort swap tests
- [ ] Invalid operation tests

Run backend tests with

```bash
cd api
mvn test
```

---

## Roadmap

### Current milestone: Production readiness

The current focus is ensuring the existing functionality is reliable before introducing new features.

### Reliability

- [ ] Increase unit test coverage
- [ ] Add solver integration tests
- [ ] Add regression tests for known bugs
- [ ] Improve exception handling
- [ ] Improve logging

### Backend

- [ ] Backend cleanup
- [ ] Remove N+1 queries
- [ ] Phantom teacher cleanup

### Frontend

- [ ] Frontend architecture refactor
- [ ] Improve sticky component animations
- [ ] Better timetable generation error reporting

### Future

- [ ] Pre-solver feasibility analysis
- [ ] Automatic Business Simulation teacher assignment
