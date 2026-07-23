import api from './api'
import type { ApiResponse } from './responses/apiResponse'
import type { CandidateTeacher } from './dto/timetable'

export interface OccupantInfo {
  scheduledClassId: number
  subjectName: string
  cohortName: string
}

export interface ValidSlot {
  timeslotId: number
  dayOfWeek: string
  startTime: string
  endTime: string
  isSwap: boolean
  displaced: OccupantInfo[]
  roomName: string
  roomId: number
}

export interface CohortSwapCandidate {
  scheduledClassId: number
  subjectName: string
  dayOfWeek: string
  startTime: string
  roomName: string
}

export const permutationService = {
  getValidSlots: async (
    scheduledClassId: number,
    academicYear: number,
    semester: number,
  ): Promise<ValidSlot[]> => {
    const res = await api.post<ValidSlot[]>('/v1/permutations/valid-slots', {
      scheduledClassId, academicYear, semester,
    })
    return res.data
  },

  applySwap: async (
    scheduledClassId: number,
    targetTimeslotId: number,
    targetRoomId: number,
    swapWithIds: number[],
  ): Promise<void> => {
    await api.post('/v1/permutations/apply', {
      scheduledClassId,
      targetTimeslotId,
      targetRoomId,
      swapWithIds,
    })
  },

  getCohortSwapCandidates: async (
    scheduledClassId: number,
    academicYear: number,
    semester: number,
  ): Promise<CohortSwapCandidate[]> => {
    const res = await api.post<CohortSwapCandidate[]>('/v1/permutations/cohort-swap/candidates', {
      scheduledClassId, academicYear, semester,
    })
    return res.data
  },

  applyCohortSwap: async (
    scheduledClassIdA: number,
    scheduledClassIdB: number,
  ): Promise<void> => {
    await api.post('/v1/permutations/cohort-swap/apply', {
      scheduledClassIdA,
      scheduledClassIdB,
    })
  },

  getTeacherCandidates: async (
    lessonId: number
  ): Promise<CandidateTeacher[]> => {
    const res = await api.get<ApiResponse<CandidateTeacher[]>>(
      `/v1/timetables/lessons/${lessonId}/candidate-teachers`
    )
    return res.data.data
  },

  reassignTeacher: async (
    lessonId: number,
    teacherId: number
  ): Promise<void> => {
    await api.patch(
      `/v1/timetables/lessons/${lessonId}/reassign-teacher`,
      { teacherId }
    )
  },
}
