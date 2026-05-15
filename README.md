# University Timetable Management System

A comprehensive automated solution for university timetable scheduling and academic management. The system utilizes constraint satisfaction algorithms to optimize resource allocation across courses, cohorts, and faculty.

## Technology Stack

### Backend
- Java 21
- Spring Boot 3.x
- Timefold Solver (Constraint Satisfaction Engine)
- Spring Security with JWT
- PostgreSQL
- Maven

### Frontend
- Vue.js 3 (Composition API)
- Vite
- Pinia (State Management)
- Tailwind CSS 4
- TypeScript
- Lucide Vue Next (Iconography)

## Project Structure

### Backend (api/)
The backend follows a modular Spring Boot architecture:
- `auth/`: Authentication logic, session management, and JWT issuance.
- `common/`: Global exception handling, response wrappers, and shared utilities.
- `config/`: System configuration including Security, CORS, and data initializers.
- `domain/`: JPA entities representing the core business model (Users, Courses, Rooms, Subjects).
- `scheduler_engine/`: The core optimization engine.
    - `domain/`: Timefold planning entities and solutions.
    - `solver/`: Constraint definitions and score calculation logic.
    - `preparation/`: Data transformation logic to prepare datasets for the solver.
- `security/`: JWT filters and security context configuration.

### Frontend (frontend/)
A modern SPA built with Vue.js 3:
- `component/`: Reusable UI components (Tables, Forms, Pagination).
- `composables/`: Shared reactive logic (e.g., toast notifications).
- `layouts/`: Application structural templates.
- `services/`: API integration layer and Data Transfer Objects (DTOs).
- `stores/`: Centralized state management using Pinia.
- `views/`: Page-level components and specific business logic.

## Domain Model

- **Cohort**: A specific group of students within a course, used for room capacity management.
- **Course**: The primary academic entity that anchors subjects and is managed by a Coordinator.
- **Room**: Physical resource with predefined capacity.
- **Subject**: Academic discipline with specific credit hours and faculty requirements.
- **Lesson Assignment**: A scheduled instance of a subject assigned to a teacher, timeslot, and room.
- **Timetable**: Aggregation of lesson assignments for a specific academic period.

## Scheduler Engine

The generation process follows three distinct phases:
1. **Data Preparation**: Validation of cohorts and greedy teacher assignment based on workload balancing.
2. **Lesson Initialization**: Creation of lesson assignments based on subject credits.
3. **Constraint Optimization**: The Timefold solver assigns optimal timeslots and rooms while respecting hard constraints (e.g., teacher/room conflicts) and soft constraints.

## Frontend Maintenance & Best Practices

To ensure long-term maintainability and scalability of the frontend, the following improvements and practices are recommended:

### 1. Project Organization & Consistency
- **Folder Naming**: Standardize on plural names for all directories in `src/` (e.g., rename `component/` to `components/` and `service/` to `services/`).
- **Service Pattern**: Align all services to use a single pattern (either strictly classes or exported object literals) to avoid architectural drift.
- **Barrel Exports**: Implement `index.ts` files in major folders (e.g., `components/ui`, `services/dto`) to simplify imports and provide a cleaner public API for modules.

### 2. Type Safety & Developer Experience
- **Eliminate `any`**: Refactor components and services to remove `any` types. Use TypeScript generics for reusable components like `CrudTable` to ensure rows and columns are properly typed.
- **API Validation**: Integrate a library like **Zod** to validate API responses at runtime, ensuring the frontend is resilient to backend schema changes.
- **Shared Constants**: Move hardcoded strings (roles like `ADMIN`, `STUDENT`, `TEACHER`) to shared constants or enums to prevent "magic strings" bugs.

### 3. UI/UX Scalability
- **Base Component Library**: Extract repeated Tailwind patterns into a set of "Base" UI components (e.g., `BaseButton`, `BaseInput`, `BaseCard`). This centralizes styling and makes theme updates easier.
- **Internationalization (i18n)**: Implement `vue-i18n` to remove hardcoded strings from templates. This simplifies maintenance and enables multi-language support (e.g., PT/EN).
- **Centralized Error Handling**: Improve the `api.ts` interceptor to automatically trigger toast notifications for common error codes, reducing boilerplate in views and stores.

### 4. Testing & Quality Assurance
- **Component Testing**: Increase coverage for core UI components (like `CrudTable` and `CrudForm`) using Vitest and Vue Test Utils.
- **Store Logic**: Add unit tests for Pinia stores to verify complex state transitions (e.g., auth flow, scheduling logic).

## Workload Calculation Analysis

The system calculates teacher workload in different contexts, with some variations in methodology:

### 1. Dashboard Statistics (`DashboardStatsService`)
- **Metric**: Uses "Lesson Blocks" (slots) via `CohortSubject::getLessonBlocksPerWeek()`.
- **Logic**: Sums the number of 2-hour sessions assigned to a teacher.
- **Comparison**: Compares total *blocks* against the teacher's *hourly* limit (e.g., comparing 6 blocks against a 12-hour limit).
- **Status**: **Inconsistent**. This approach underestimates actual workload and may fail to correctly identify overloaded teachers.

### 2. Timetable Generation (`TeacherAssignmentService`)
- **Metric**: Uses "Weekly Hours" via `CohortSubject::getWeeklyHours()`.
- **Logic**: Correctly calculates total contact hours per week.
- **Comparison**: Compares total hours against the teacher's limit as defined in `AcademicPolicy`.
- **Status**: **Correct**. Used for greedy assignment and phantom teacher fallback.

### 3. Phantom Teacher Replacement (`TimetableService`)
- **Metric**: Uses "Weekly Hours" via `CohortSubject::getWeeklyHours()`.
- **Logic**: Aggregates hours for all active cohort subjects in the period.
- **Comparison**: Evaluates if a replacement teacher would exceed their hourly limit before suggesting them as a candidate.
- **Status**: **Correct**. Ensures manual reassignments respect academic policies.

### 4. Subject Assignment (`CohortSubjectService`)
- **Metric**: Uses "Weekly Hours" via `CohortSubject::getWeeklyHours()`.
- **Logic**: Real-time validation during CRUD operations.
- **Status**: **Correct**. Prevents illegal assignments via the API.

## Testing

Unit tests are provided for core services. To execute tests:

```bash
cd api
mvn test -Dtest="*ServiceTest"
```

TODO:
- [x] analitics by academicPeriodDto on dashboard
- [x] sticky page headers and filters for dashboard course and timetable page.
- [x] Fix workload calculation on dashboard
- [ ] granular workloads (hh && mm)
- [ ] Swap phantom -> teacher should allow going over contract limits
- [ ] Fix dashboard insight not loading on page entering
- [ ] Fix timetable generation (should not stop when i leave the page)
- [ ] smooth sticky component animations to they dont block scrolling
- [x] phantom swap with real teacher
- [ ] refactor backend (proper exception handling, logging, clean code, spring conventions, optimization no N+1s)
- [x] Dashboard with better info (more usefull ie: teacher workloads room ocupation and so on)
- [x] Cohort management (improve confirmation it should scale with the room capacity, so i have to be mindfull of how big cohorts can get withouth breaking the system)
- [ ] pre-solver awarness (compute cohorts and rooms to derive the likelyhood of generating a valid solution. Informative only on the "ui" not restrictive "solver")
- [ ] Phantom cleanUp(on swap with real teacher)
- [x] Notification tab improvments
- [ ] frontend refactor (match backend)

