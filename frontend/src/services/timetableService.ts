import api from './api'
import type {
  TimetableSolution,
  GenerationStartedResponse,
  JobStatusResponse,
  AcademicPeriodDto,
} from './dto/timetable'

export const timetableService = {
  generate: async (academicYear: number, semester: number): Promise<GenerationStartedResponse> => {
    const res = await api.post<GenerationStartedResponse>('/v1/solver/generate', {
      academicYear,
      semester,
    })
    return res.data
  },

  getSolution: async (jobId: string): Promise<TimetableSolution | null> => {
    const res = await api.get(`/v1/solver/${jobId}`)
    if (res.status === 202) return null
    return res.data as TimetableSolution
  },

  /**
   * Loads a persisted timetable from the DB by year + semester.
   * Returns null if not yet generated (404).
   */
  loadPersisted: async (
    academicYear: number,
    semester: number,
  ): Promise<TimetableSolution | null> => {
    try {
      const res = await api.get(`/v1/timetables/${academicYear}/${semester}`)
      return res.data as TimetableSolution
    } catch (e: any) {
      if (e?.response?.status === 404) return null
      throw e
    }
  },

  async getAvailablePeriods(): Promise<AcademicPeriodDto[]> {
    const res = await api.get('/v1/timetables/periods')

    return res.data.data
  },

  loadMyStudentTimetable: async (
    academicYear: number,
    semester: number,
  ): Promise<TimetableSolution | null> => {
    try {
      const res = await api.get(`/v1/timetables/me/student`, {
        params: { year: academicYear, semester },
      })
      return res.data as TimetableSolution
    } catch (e: any) {
      if (e?.response?.status === 404) return null
      throw e
    }
  },

  loadMyTeacherTimetable: async (
    academicYear: number,
    semester: number,
  ): Promise<TimetableSolution | null> => {
    try {
      const res = await api.get(`/v1/timetables/me/teacher`, {
        params: { year: academicYear, semester },
      })
      return res.data as TimetableSolution
    } catch (e: any) {
      if (e?.response?.status === 404) return null
      throw e
    }
  },

  getStatus: async (jobId: string): Promise<JobStatusResponse> => {
    const res = await api.get<JobStatusResponse>(`/v1/solver/${jobId}/status`)
    return res.data
  },

  async submitForApproval(id: number) {
    const { data } = await api.post(`/v1/timetables/${id}/submit`)
    return data
  },
  async approve(id: number) {
    const { data } = await api.post(`/v1/timetables/${id}/approve`)
    return data
  },
  async reject(id: number) {
    const { data } = await api.post(`/v1/timetables/${id}/reject`)
    return data
  },
  async publish(id: number) {
    const { data } = await api.post(`/v1/timetables/${id}/publish`)
    return data
  },
}
