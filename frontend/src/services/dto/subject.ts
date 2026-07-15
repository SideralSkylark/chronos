export interface CreateSubjectRequest {
  name: string
  credits: number
  targetYear: number
  targetSemester: number
  courseId: number
  eligibleTeacherIds: number[]
}

export interface UpdateSubjectRequest {
  name: string
  credits: number
  targetYear: number
  targetSemester: number
  eligibleTeacherIds: number[]
}

export interface SubjectDetailResponse {
  id: number
  name: string
  credits: number
  targetYear: number
  targetSemester: number
  courseId: number
  courseName: string
  eligibleTeachers: TeacherInfo[]
}

export interface SubjectListResponse {
  id: number
  name: string
  teachers: TeacherInfo[]
}

export interface TeacherInfo {
  id: number
  username: string
}

export interface OptionalGroupResponse {
  id: number
  name: string
  subjects: number[]
}

export interface CreateOptionalGroupRequest {
  s1: number
  s2: number
}
