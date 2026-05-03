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

## Testing

Unit tests are provided for core services. To execute tests:

```bash
cd api
mvn test -Dtest="*ServiceTest"
```

The following N+1 query patterns have been identified in the backend and require optimization (e.g., using `JOIN FETCH` or `@EntityGraph`):

1. **`CohortSubjectRepository.findByAcademicYearAndSemesterAndIsActive`**
   - **Usage**: `PreSolverService` and `TeacherAssignmentService`.
   - **Problem**: Iterating over the results to calculate teacher workloads or generate display names triggers separate queries for `Cohort`, `Subject`, and `ApplicationUser` (assignedTeacher) for *each* `CohortSubject`.
   - **Impact**: High. This occurs during the critical data preparation phase of the scheduler engine.

2. **`SubjectRepository.search` (and other finders like `findByTargetYearAndTargetSemester`)**
   - **Usage**: `SubjectController` (list views).
   - **Problem**: Returns a list of `Subject` without fetching the associated `Course` or the `eligibleTeachers` collection.
   - **Impact**: Medium. Causes multiple queries when displaying a list of subjects with their course names or eligible teacher counts.

3. **`CohortRepository` list finders (e.g., `findByCourseId`, `findByAcademicYear`)**
   - **Usage**: `CohortController`.
   - **Problem**: Fetches `Cohort` entities without joining the `Course` or the `students` collection.
   - **Impact**: Medium. Accessing `getStudentCount()` (accesses `students.size()`) or `getDisplayName()` (accesses `course.getName()`) triggers an N+1 for each cohort in the list.

4. **`ScheduledClassRepository.findByCohortId` (and `findByTeacherId`, `findByCohortSubjectId`)**
   - **Usage**: `ScheduledClassController`.
   - **Problem**: These methods use simple JPQL that does not `JOIN FETCH` related entities like `cohortSubject`, `room`, `timeslot`, or `timetable`.
   - **Impact**: Medium. Displays of scheduled classes in the UI will trigger separate queries for each row's details.

5. **`UserRepository.findAllByRole` (and Specification-based `findAll`)**
   - **Usage**: `UserService.getAllUsers`.
   - **Problem**: `ApplicationUser.roles` is marked as `FetchType.EAGER`, but the queries often do not use `FETCH JOIN`.
   - **Impact**: Low/Medium. Depending on Hibernate's execution plan, it may fetch roles in separate queries for each user in the result list.

---
## Dashboard Enrichment — Implementation Plan

### Step 1 — Current period endpoint (prerequisite for everything else)

Add `GET /api/v1/dashboard/current-period` returning:
```json
{ "academicYear": 2025, "label": "2025" }
```
Derive the year from the server clock: if current month >= September,
year = current year, else year = current year - 1. This matches the
standard academic year convention. No database call needed.

On the frontend, add `currentYear` and `currentLabel` to the dashboard
store and call this endpoint on mount before rendering any widget. maybe we dont need a label since it will always match the academic year

---

### Step 2 — Timetable status summary endpoint

Add `GET /api/v1/dashboard/timetable-status?year={year}` returning:
```json
[
  { "semester": 1, "status": "PUBLISHED", "lessonCount": 284, "conflictCount": 0, "score": "94.2" },
  { "semester": 2, "status": null }
]
```
`status: null` means no timetable exists for that semester.
Derive from the existing `TimetableSolution` entity — no new data needed.
This card always shows both semesters and is visible to all staff roles.

---

### Step 3 — Teacher workload endpoint (semester-scoped)

Add `GET /api/v1/dashboard/teacher-workload?year={year}&semester={semester}`
returning the top 10 teachers by total weekly hours for that semester,
computed from `LessonAssignment` grouped by teacher.

```json
[
  { "teacherId": 1, "fullName": "A. Santos", "initials": "AS", "weeklyHours": 22, "high": true }
]
```

Flag `high: true` when weeklyHours exceeds a configurable threshold
(default 20h — add to application.properties as
`dashboard.teacher.high-load-threshold=20`).

Visibility: ADMIN, DIRECTOR, ASISTENT. For COORDINATOR: scope the query
to only teachers assigned to subjects within their courses — add a
`?courseId={id}` optional filter and enforce it server-side based on
the caller's role.

---

### Step 4 — Room usage endpoint (semester-scoped)

Add `GET /api/v1/dashboard/room-usage?year={year}&semester={semester}`
returning rooms ranked by number of lesson assignments, plus rooms with
zero assignments.

```json
{
  "ranked": [
    { "roomId": 1, "name": "Sala A101", "lessonCount": 44, "maxCount": 44 }
  ],
  "unused": [
    { "roomId": 7, "name": "Sala C302" }
  ]
}
```

`maxCount` is the highest value in the list — used by the frontend to
compute bar widths as `(lessonCount / maxCount) * 100`.

Visibility: ADMIN, DIRECTOR, ASISTENT only.

---

### Step 5 — Course cohort breakdown endpoint (structural, no semester)

Add `GET /api/v1/dashboard/course-cohorts?year={year}` returning each
course with its cohort count for the current academic year.

```json
[
  { "courseId": 1, "courseName": "Eng. Informática", "cohortCount": 8 }
]
```

For COORDINATOR: return only their courses. Enforce server-side.
For ADMIN/DIRECTOR/ASISTENT: return all, sorted by cohortCount desc.
This widget has no semester toggle — it is structural data.

---

### Step 6 — Coordinator cohort detail endpoint

Add `GET /api/v1/dashboard/my-cohorts?year={year}&semester={semester}`
returning the cohorts belonging to the coordinator's courses, with
lesson counts from the timetable for that semester if one exists.

```json
[
  {
    "cohortId": 3,
    "displayName": "1ª Turma · 1º Ano",
    "courseName": "Eng. Informática",
    "studentCount": 32,
    "lessonCount": 18,
    "timetableStatus": "PUBLISHED"
  }
]
```

`lessonCount` and `timetableStatus` are null if no timetable exists
for that semester. Enforce coordinator ownership server-side.

---

### Step 7 — Frontend dashboard store

Create `src/stores/dashboard.ts` with:

- `currentPeriod` — loaded once on mount from Step 1
- `timetableStatus` — loaded from Step 2, refreshed on semester change
- `selectedSemester` — ref(1), drives all semester-scoped widgets
- `teacherWorkload` — loaded from Step 3, reactive to selectedSemester
- `roomUsage` — loaded from Step 4, reactive to selectedSemester
- `courseCohorts` — loaded from Step 5, no semester dependency
- `myCohorts` — loaded from Step 6 (coordinator only), reactive to selectedSemester
- `loading` — per-widget loading flags as a Record<string, boolean>

Watch `selectedSemester` and re-fetch Steps 3, 4, 6 automatically.

---

### Step 8 — Frontend view implementation

Enhance content in `Dashboard.vue` with live data from
the dashboard store. Implement the semester toggle component. Show
appropriate empty states when `status: null` for semester-scoped widgets.
Respect role visibility rules client-side (mirroring server enforcement):

| Widget | ADMIN | DIRECTOR | ASISTENT | COORDINATOR |
|---|---|---|---|---|
| Stats grid | ✓ all | ✓ all | ✓ rooms+cohorts | ✗ (show own counts) |
| Timetable status | ✓ | ✓ | ✓ | ✓ (read-only) |
| Teacher workload | ✓ all | ✓ all | ✓ all | ✓ own courses only |
| Room usage | ✓ | ✓ | ✓ | ✗ |
| Course cohorts | ✓ all | ✓ all | ✓ all | ✓ own courses only |
| My cohorts detail | ✗ | ✗ | ✗ | ✓ |

---

