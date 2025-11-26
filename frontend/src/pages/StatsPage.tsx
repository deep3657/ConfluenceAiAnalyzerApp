import { useState, useEffect } from 'react'
import { BarChart3, RefreshCw, Loader2, FileText, CheckCircle, Clock, AlertCircle, XCircle } from 'lucide-react'
import { api, StatsResponse } from '../services/api'

export default function StatsPage() {
  const [stats, setStats] = useState<StatsResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null)

  const fetchStats = async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await api.getStats()
      setStats(data)
      setLastUpdated(new Date())
    } catch (err: any) {
      setError(err.response?.data?.message || err.message || 'Failed to fetch statistics')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchStats()
    // Auto-refresh every 30 seconds
    const interval = setInterval(fetchStats, 30000)
    return () => clearInterval(interval)
  }, [])

  if (loading && !stats) {
    return (
      <div className="flex items-center justify-center h-64">
        <Loader2 className="h-8 w-8 animate-spin text-primary-600" />
      </div>
    )
  }

  if (error && !stats) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-lg p-6">
        <div className="flex items-start space-x-3">
          <XCircle className="h-5 w-5 text-red-600 flex-shrink-0 mt-0.5" />
          <div>
            <h3 className="text-sm font-medium text-red-800">Error</h3>
            <p className="text-sm text-red-700 mt-1">{error}</p>
            <button
              onClick={fetchStats}
              className="mt-3 text-sm text-red-600 hover:text-red-800 underline"
            >
              Try again
            </button>
          </div>
        </div>
      </div>
    )
  }

  if (!stats) return null

  const statusData = [
    { label: 'Pending', value: stats.pagesByStatus.PENDING, icon: Clock, color: 'bg-yellow-500' },
    { label: 'Parsed', value: stats.pagesByStatus.PARSED, icon: FileText, color: 'bg-blue-500' },
    { label: 'Embedded', value: stats.pagesByStatus.EMBEDDED, icon: CheckCircle, color: 'bg-green-500' },
    { label: 'Error', value: stats.pagesByStatus.ERROR, icon: AlertCircle, color: 'bg-red-500' },
  ]

  const totalProcessed = stats.pagesByStatus.PARSED + stats.pagesByStatus.EMBEDDED
  const successRate = stats.totalPages > 0
    ? Math.round((totalProcessed / stats.totalPages) * 100)
    : 0

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-3xl font-bold text-gray-900">Statistics</h2>
          <p className="mt-2 text-gray-600">
            Overview of ingested and processed RCA documents
          </p>
        </div>
        <button
          onClick={fetchStats}
          disabled={loading}
          className="flex items-center space-x-2 px-4 py-2 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        >
          <RefreshCw className={`h-5 w-5 ${loading ? 'animate-spin' : ''}`} />
          <span>Refresh</span>
        </button>
      </div>

      {lastUpdated && (
        <p className="text-sm text-gray-500">
          Last updated: {lastUpdated.toLocaleTimeString()}
        </p>
      )}

      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600">Total Pages</p>
              <p className="text-3xl font-bold text-gray-900 mt-2">{stats.totalPages}</p>
            </div>
            <div className="p-3 bg-primary-100 rounded-lg">
              <BarChart3 className="h-6 w-6 text-primary-600" />
            </div>
          </div>
        </div>

        <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600">Success Rate</p>
              <p className="text-3xl font-bold text-gray-900 mt-2">{successRate}%</p>
            </div>
            <div className="p-3 bg-green-100 rounded-lg">
              <CheckCircle className="h-6 w-6 text-green-600" />
            </div>
          </div>
        </div>

        <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600">Processed</p>
              <p className="text-3xl font-bold text-gray-900 mt-2">{totalProcessed}</p>
            </div>
            <div className="p-3 bg-blue-100 rounded-lg">
              <FileText className="h-6 w-6 text-blue-600" />
            </div>
          </div>
        </div>

        <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600">Errors</p>
              <p className="text-3xl font-bold text-gray-900 mt-2">{stats.pagesByStatus.ERROR}</p>
            </div>
            <div className="p-3 bg-red-100 rounded-lg">
              <AlertCircle className="h-6 w-6 text-red-600" />
            </div>
          </div>
        </div>
      </div>

      {/* Status Breakdown */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Status Breakdown</h3>
        <div className="space-y-4">
          {statusData.map((item) => {
            const Icon = item.icon
            const percentage = stats.totalPages > 0
              ? Math.round((item.value / stats.totalPages) * 100)
              : 0

            return (
              <div key={item.label}>
                <div className="flex items-center justify-between mb-2">
                  <div className="flex items-center space-x-3">
                    <Icon className="h-5 w-5 text-gray-600" />
                    <span className="font-medium text-gray-700">{item.label}</span>
                  </div>
                  <div className="flex items-center space-x-3">
                    <span className="text-sm text-gray-600">{percentage}%</span>
                    <span className="font-semibold text-gray-900">{item.value}</span>
                  </div>
                </div>
                <div className="w-full bg-gray-200 rounded-full h-2">
                  <div
                    className={`${item.color} h-2 rounded-full transition-all duration-300`}
                    style={{ width: `${percentage}%` }}
                  />
                </div>
              </div>
            )
          })}
        </div>
      </div>

      {/* Processing Pipeline Visualization */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Processing Pipeline</h3>
        <div className="flex items-center justify-between">
          <PipelineStage
            label="Pending"
            count={stats.pagesByStatus.PENDING}
            color="yellow"
          />
          <div className="flex-1 h-0.5 bg-gray-300 mx-4" />
          <PipelineStage
            label="Parsed"
            count={stats.pagesByStatus.PARSED}
            color="blue"
          />
          <div className="flex-1 h-0.5 bg-gray-300 mx-4" />
          <PipelineStage
            label="Embedded"
            count={stats.pagesByStatus.EMBEDDED}
            color="green"
          />
        </div>
      </div>
    </div>
  )
}

function PipelineStage({ label, count, color }: { label: string; count: number; color: string }) {
  const colorClasses = {
    yellow: 'bg-yellow-500',
    blue: 'bg-blue-500',
    green: 'bg-green-500',
  }

  return (
    <div className="flex flex-col items-center">
      <div className={`w-16 h-16 ${colorClasses[color as keyof typeof colorClasses]} rounded-full flex items-center justify-center text-white font-bold text-lg mb-2`}>
        {count}
      </div>
      <span className="text-sm font-medium text-gray-700">{label}</span>
    </div>
  )
}

