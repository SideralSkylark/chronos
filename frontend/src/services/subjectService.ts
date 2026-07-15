import api from './api'
import type { ApiResponse } from './responses/apiResponse'
import type { Page } from './types/page'
import type {
  CreateSubjectRequest,
  UpdateSubjectRequest,
  SubjectDetailResponse,
  OptionalGroupResponse,
  CreateOptionalGroupRequest,
} from './dto/subject'

const BASE_URL = '/v1/subjects'

export const subjectService = {
  async create(data: CreateSubjectRequest) {
    const res = await api.post<ApiResponse<SubjectDetailResponse>>(BASE_URL, data)
    return res.data.data
  },

  async createOptionalGroup(data: CreateOptionalGroupRequest) {
    const res = await api.post<ApiResponse<OptionalGroupResponse>>(
      `${BASE_URL}/optional-groups`,
      data,
    )
    return res.data.data
  },

  async getAllByCourse(courseId: number, page = 0, size = 10) {
    const res = await api.get<ApiResponse<Page<SubjectDetailResponse>>>(
      `${BASE_URL}/course/${courseId}`,
      {
        params: { page, size },
      },
    )

    return res.data.data
  },

  async getOptionalGroups(page = 0, size = 10) {
    const res = await api.get<ApiResponse<Page<OptionalGroupResponse>>>(
      `${BASE_URL}/optional-groups`,
      {
        params: { page, size },
      },
    )

    return res.data.data
  },

  async getById(id: number) {
    const res = await api.get<ApiResponse<SubjectDetailResponse>>(`${BASE_URL}/${id}`)
    return res.data.data
  },

  async update(id: number, data: UpdateSubjectRequest) {
    const res = await api.put<ApiResponse<SubjectDetailResponse>>(`${BASE_URL}/${id}`, data)
    return res.data.data
  },

  async delete(id: number) {
    await api.delete(`${BASE_URL}/${id}`)
  },

  async deleteOptionalGroup(id: number) {
    await api.delete(`${BASE_URL}/optional-groups/${id}`)
  },
}
