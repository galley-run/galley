import axios, { AxiosError, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'

type ApiErrorBody = { errors?: Array<{ title: string, code: number }> ; error?: string };

export class ApiError<T = unknown, D = unknown> extends Error {
  status?: number
  body?: ApiErrorBody
  config?: InternalAxiosRequestConfig<D>
  request?: unknown
  response?: AxiosResponse<T, D>

  constructor(
    message: string,
    status?: number,
    body?: ApiErrorBody,
    config?: InternalAxiosRequestConfig<D>,
    request?: unknown,
    response?: AxiosResponse<T, D>,
  ) {
    super(message)
    this.name = ''
    this.status = status
    this.body = body
    this.config = config
    this.request = request
    this.response = response
  }
}

export default function registerAxios() {
  axios.defaults.baseURL = import.meta.env.VITE_API_URL
  // axios.defaults.headers.common['Authorization'] = `Bearer ${import.meta.env.VITE_API_TOKEN}`
  axios.defaults.headers.common['Accept'] = `application/vnd.galley.v1+json`
  axios.defaults.headers.common['Content-Type'] = `application/vnd.galley.v1+json`

  axios.interceptors.response.use(
    (res) => {
      if (res.status === 203) {
        console.error('Response Validation Incorrect (for:)', res)
      }
      return res?.data
    },
    (e: AxiosError<ApiErrorBody>) => {
      const body = e.response?.data
      const msg = body?.errors?.[0]?.title || body?.error || e.message || 'Er ging iets mis'

      return Promise.reject(
        new ApiError(
          msg,
          body?.errors?.[0]?.code ?? e.response?.status ?? 0,
          body,
          e.response?.config,
          e.request,
          e.response,
        ),
      )
    },
  )
}
