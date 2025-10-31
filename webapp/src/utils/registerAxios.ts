import axios, { type AxiosError } from 'axios'

type ApiErrorBody = { errors?: Array<{ title: string, code: number }> ; error?: string };

class ApiError extends Error {
  status?: number;
  body?: ApiErrorBody;
  constructor(message: string, status?: number, body?: ApiErrorBody) {
    super(message);
    this.name = "";
    this.status = status;
    this.body = body;
  }
}

export default function registerAxios() {
  axios.defaults.baseURL = import.meta.env.VITE_API_URL
  // axios.defaults.headers.common['Authorization'] = `Bearer ${import.meta.env.VITE_API_TOKEN}`
  axios.defaults.headers.common['Accept'] = `application/vnd.galley.v1+json`
  axios.defaults.headers.common['Content-Type'] = `application/vnd.galley.v1+json`

  axios.interceptors.response.use(
    (res) => res,
    (e: AxiosError<ApiErrorBody>) => {
      const body = e.response?.data
      const msg =
        body?.errors?.[0]?.title ||
        body?.error ||
        e.message ||
        'Er ging iets mis'
      return Promise.reject(new ApiError(msg, body?.errors?.[0]?.code ?? e.response?.status, body))
    },
  )
}
