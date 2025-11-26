import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request interceptor for logging
apiClient.interceptors.request.use(
  (config) => {
    console.log(`[API] ${config.method?.toUpperCase()} ${config.url}`)
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Response interceptor for error handling
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('[API Error]', error.response?.data || error.message)
    return Promise.reject(error)
  }
)

export interface SearchRequest {
  query: string
  topK?: number
}

export interface SearchResult {
  pageId: string
  title: string
  url: string
  similarityScore: number
  symptoms?: string
  rootCause?: string
  resolution?: string
  incidentDate?: string
}

export interface SearchSummary {
  suggestedRootCause: string
  confidence: 'High' | 'Medium' | 'Low'
  similarIncidents: number
}

export interface SearchResponse {
  query: string
  results: SearchResult[]
  summary: SearchSummary
  executionTimeMs: number
}

export interface SyncRequest {
  syncType: 'FULL' | 'INCREMENTAL'
  spaces?: string[]
  tags?: string[]
}

export interface SyncResponse {
  syncId: string
  status: 'RUNNING' | 'COMPLETED' | 'FAILED'
  message: string
  pagesFetched: number
  pagesProcessed: number
  pagesFailed: number
  startedAt: string
  completedAt?: string
}

export interface StatsResponse {
  totalPages: number
  pagesByStatus: {
    PENDING: number
    PARSED: number
    EMBEDDED: number
    ERROR: number
  }
}

export interface RcaPage {
  pageId: string
  spaceKey: string
  title: string
  url: string
  tags: string[]
  lastModified: string
  ingestedAt: string
  parsedAt?: string
  embeddingGeneratedAt?: string
  status: 'PENDING' | 'PARSED' | 'EMBEDDED' | 'ERROR'
  errorMessage?: string
}

export const api = {
  // Search endpoints
  search: async (request: SearchRequest): Promise<SearchResponse> => {
    const response = await apiClient.post<SearchResponse>('/v1/search', request)
    return response.data
  },

  searchBySymptoms: async (request: SearchRequest): Promise<SearchResponse> => {
    const response = await apiClient.post<SearchResponse>('/v1/search/symptoms', request)
    return response.data
  },

  searchByRootCause: async (request: SearchRequest): Promise<SearchResponse> => {
    const response = await apiClient.post<SearchResponse>('/v1/search/root-cause', request)
    return response.data
  },

  // Ingestion endpoints
  startSync: async (request: SyncRequest): Promise<SyncResponse> => {
    const response = await apiClient.post<SyncResponse>('/v1/ingestion/sync', request)
    return response.data
  },

  getSyncStatus: async (syncId: string): Promise<SyncResponse> => {
    const response = await apiClient.get<SyncResponse>(`/v1/ingestion/sync/${syncId}`)
    return response.data
  },

  ingestPage: async (pageId: string): Promise<void> => {
    await apiClient.post(`/v1/ingestion/page/${pageId}`)
  },

  // Management endpoints
  getStats: async (): Promise<StatsResponse> => {
    const response = await apiClient.get<StatsResponse>('/v1/stats')
    return response.data
  },

  getPage: async (pageId: string): Promise<RcaPage> => {
    const response = await apiClient.get<RcaPage>(`/v1/pages/${pageId}`)
    return response.data
  },

  // Health check
  health: async (): Promise<{ status: string; timestamp: string; service: string }> => {
    const response = await apiClient.get('/health')
    return response.data
  },
}

export default api

