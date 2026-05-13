export interface GenerationStartedResponse {
  jobId: string
  message: string
}

export interface AcademicPeriodDto {
  year: number
  semesters: number[]
}

export interface JobStatusResponse {
  jobId: string
  status: 'SOLVING_ACTIVE' | 'SOLVING_SCHEDULED' | 'NOT_SOLVING'
  score: string | null
  message: string
}

export interface TimetableSolution {
  id: number
  lessonAssignments: LessonAssignment[]
  score: string
  academicYear: number
  semester: number
  status: 'DRAFT' | 'PENDING_APPROVAL' | 'APPROVED' | 'PUBLISHED'
  identifier: string
  unassignedLessons: number
  totalLessons: number
  feasible: boolean
}

export interface LessonAssignment {
  id: number
  blockNumber: number
  assigned: boolean
  timeslot: TimeslotInfo | null
  room: RoomInfo | null
  cohort: CohortInfo
  subject: SubjectInfo
  teacher: TeacherInfo
  studentCount: number
  courseId: number
}

export interface TimeslotInfo {
  id: number
  dayOfWeek: string
  startTime: string
  endTime: string
  displayName: string
  period: 'MORNING' | 'AFTERNOON' | 'EVENING'
  dayNumber: number
}

export interface RoomInfo {
  id: number
  name: string
  capacity: number
}

export interface CohortInfo {
  id: number
  displayName: string
  studentCount: number
  courseId: number
  year: number
  section: string
}

export interface SubjectInfo {
  id: number
  name: string
  credits: number
  targetYear: number
  targetSemester: number
}

export interface TeacherInfo {
  id: number
  name: string
  fullName: string
  email: string
  username?: string
}

export interface CandidateTeacher {
  teacherId: number
  username: string
  currentWeeklyHours: number
  weeklyLimit: number
  wouldExceed: boolean
  isEligible: boolean
}
