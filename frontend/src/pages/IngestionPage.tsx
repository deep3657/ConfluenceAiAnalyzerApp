import { useState, useEffect } from 'react'
import { Database, Play, RefreshCw, Loader2, CheckCircle, XCircle, Clock } from 'lucide-react'
import { api, SyncRequest, SyncResponse } from '../services/api'
import { format } from 'date-fns'

export default function IngestionPage() {
  const [syncType, setSyncType] = useState<'FULL' | 'INCREMENTAL'>('INCREMENTAL')
  const [spaces, setSpaces] = useState('ENG,OPS')
  const [tags, setTags] = useState('rca,post-mortem')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [activeSyncs, setActiveSyncs] = useState<Map<string, SyncResponse>>(new Map())

  const handleStartSync = async () => {
    setLoading(true)
    setError(null)

    try {
      const request: SyncRequest = {
        syncType,
        spaces: spaces.split(',').map(s => s.trim()).filter(s => s),
        tags: tags.split(',').map(t => t.trim()).filter(t => t),
      }

      const response = await api.startSync(request)
      setActiveSyncs(prev => new Map(prev.set(response.syncId, response)))
      
      // Start polling for status
      pollSyncStatus(response.syncId)
    } catch (err: any) {
      setError(err.response?.data?.message || err.message || 'Failed to start sync')
    } finally {
      setLoading(false)
    }
  }

  const pollSyncStatus = async (syncId: string) => {
    const interval = setInterval(async () => {
      try {
        const status = await api.getSyncStatus(syncId)
        setActiveSyncs(prev => {
          const updated = new Map(prev)
          updated.set(syncId, status)
          return updated
        })

        // Stop polling if sync is completed or failed
        if (status.status === 'COMPLETED' || status.status === 'FAILED') {
          clearInterval(interval)
        }
      } catch (err) {
        console.error('Error polling sync status:', err)
        clearInterval(interval)
      }
    }, 2000) // Poll every 2 seconds
  }

  useEffect(() => {
    // Cleanup intervals on unmount
    return () => {
      // Intervals will be cleared when syncs complete
    }
  }, [])

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-3xl font-bold text-gray-900">Data Ingestion</h2>
        <p className="mt-2 text-gray-600">
          Sync and ingest RCA pages from Confluence
        </p>
      </div>

      {/* Sync Form */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Sync Type
            </label>
            <select
              value={syncType}
              onChange={(e) => setSyncType(e.target.value as 'FULL' | 'INCREMENTAL')}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            >
              <option value="INCREMENTAL">Incremental (only new/updated pages)</option>
              <option value="FULL">Full (all pages)</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Spaces (comma-separated)
            </label>
            <input
              type="text"
              value={spaces}
              onChange={(e) => setSpaces(e.target.value)}
              placeholder="ENG, OPS"
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Tags (comma-separated)
            </label>
            <input
              type="text"
              value={tags}
              onChange={(e) => setTags(e.target.value)}
              placeholder="rca, post-mortem"
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            />
          </div>

          <button
            onClick={handleStartSync}
            disabled={loading}
            className="w-full bg-primary-600 text-white px-6 py-3 rounded-lg font-medium hover:bg-primary-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition-colors flex items-center justify-center space-x-2"
          >
            {loading ? (
              <>
                <Loader2 className="h-5 w-5 animate-spin" />
                <span>Starting Sync...</span>
              </>
            ) : (
              <>
                <Play className="h-5 w-5" />
                <span>Start Sync</span>
              </>
            )}
          </button>
        </div>
      </div>

      {/* Error Message */}
      {error && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 flex items-start space-x-3">
          <XCircle className="h-5 w-5 text-red-600 flex-shrink-0 mt-0.5" />
          <div>
            <h3 className="text-sm font-medium text-red-800">Error</h3>
            <p className="text-sm text-red-700 mt-1">{error}</p>
          </div>
        </div>
      )}

      {/* Active Syncs */}
      {activeSyncs.size > 0 && (
        <div>
          <h3 className="text-xl font-semibold text-gray-900 mb-4">
            Active Sync Operations ({activeSyncs.size})
          </h3>
          <div className="space-y-4">
            {Array.from(activeSyncs.values()).map((sync) => (
              <SyncStatusCard key={sync.syncId} sync={sync} />
            ))}
          </div>
        </div>
      )}
    </div>
  )
}

function SyncStatusCard({ sync }: { sync: SyncResponse }) {
  const getStatusIcon = () => {
    switch (sync.status) {
      case 'COMPLETED':
        return <CheckCircle className="h-5 w-5 text-green-600" />
      case 'FAILED':
        return <XCircle className="h-5 w-5 text-red-600" />
      case 'RUNNING':
        return <Loader2 className="h-5 w-5 text-primary-600 animate-spin" />
      default:
        return <Clock className="h-5 w-5 text-gray-600" />
    }
  }

  const getStatusColor = () => {
    switch (sync.status) {
      case 'COMPLETED':
        return 'bg-green-100 text-green-800 border-green-200'
      case 'FAILED':
        return 'bg-red-100 text-red-800 border-red-200'
      case 'RUNNING':
        return 'bg-blue-100 text-blue-800 border-blue-200'
      default:
        return 'bg-gray-100 text-gray-800 border-gray-200'
    }
  }

  const progress = sync.pagesFetched > 0
    ? Math.round((sync.pagesProcessed / sync.pagesFetched) * 100)
    : 0

  return (
    <div className={`bg-white rounded-lg shadow-sm border-2 ${getStatusColor()} p-6`}>
      <div className="flex items-start justify-between mb-4">
        <div className="flex items-center space-x-3">
          {getStatusIcon()}
          <div>
            <h4 className="font-semibold">Sync ID: {sync.syncId.substring(0, 8)}...</h4>
            <p className="text-sm opacity-75">{sync.message}</p>
          </div>
        </div>
        <span className={`px-3 py-1 rounded-full text-sm font-medium ${getStatusColor()}`}>
          {sync.status}
        </span>
      </div>

      {sync.status === 'RUNNING' && (
        <div className="mb-4">
          <div className="flex justify-between text-sm mb-1">
            <span>Progress</span>
            <span>{progress}%</span>
          </div>
          <div className="w-full bg-gray-200 rounded-full h-2">
            <div
              className="bg-primary-600 h-2 rounded-full transition-all duration-300"
              style={{ width: `${progress}%` }}
            />
          </div>
        </div>
      )}

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
        <div>
          <span className="text-gray-600">Pages Fetched:</span>
          <span className="ml-2 font-semibold">{sync.pagesFetched}</span>
        </div>
        <div>
          <span className="text-gray-600">Pages Processed:</span>
          <span className="ml-2 font-semibold text-green-600">{sync.pagesProcessed}</span>
        </div>
        <div>
          <span className="text-gray-600">Pages Failed:</span>
          <span className="ml-2 font-semibold text-red-600">{sync.pagesFailed}</span>
        </div>
        <div>
          <span className="text-gray-600">Started:</span>
          <span className="ml-2 font-semibold">
            {format(new Date(sync.startedAt), 'MMM d, HH:mm')}
          </span>
        </div>
      </div>

      {sync.completedAt && (
        <div className="mt-4 pt-4 border-t border-gray-200 text-sm">
          <span className="text-gray-600">Completed:</span>
          <span className="ml-2 font-semibold">
            {format(new Date(sync.completedAt), 'MMM d, yyyy HH:mm:ss')}
          </span>
        </div>
      )}
    </div>
  )
}

