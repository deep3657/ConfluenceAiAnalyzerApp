import { useState } from 'react'
import { Search, Loader2, AlertCircle, ExternalLink, Calendar, TrendingUp } from 'lucide-react'
import { api, SearchResponse, SearchResult } from '../services/api'
import { format } from 'date-fns'

type SearchType = 'general' | 'symptoms' | 'root-cause'

export default function SearchPage() {
  const [query, setQuery] = useState('')
  const [topK, setTopK] = useState(5)
  const [searchType, setSearchType] = useState<SearchType>('general')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [results, setResults] = useState<SearchResponse | null>(null)

  const handleSearch = async () => {
    if (!query.trim()) {
      setError('Please enter a search query')
      return
    }

    setLoading(true)
    setError(null)
    setResults(null)

    try {
      let response: SearchResponse
      switch (searchType) {
        case 'symptoms':
          response = await api.searchBySymptoms({ query, topK })
          break
        case 'root-cause':
          response = await api.searchByRootCause({ query, topK })
          break
        default:
          response = await api.search({ query, topK })
      }
      setResults(response)
    } catch (err: any) {
      setError(err.response?.data?.message || err.message || 'Failed to perform search')
    } finally {
      setLoading(false)
    }
  }

  const getConfidenceColor = (confidence: string) => {
    switch (confidence) {
      case 'High':
        return 'bg-green-100 text-green-800'
      case 'Medium':
        return 'bg-yellow-100 text-yellow-800'
      case 'Low':
        return 'bg-red-100 text-red-800'
      default:
        return 'bg-gray-100 text-gray-800'
    }
  }

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-3xl font-bold text-gray-900">Search RCA Documents</h2>
        <p className="mt-2 text-gray-600">
          Find similar root cause analyses using semantic search powered by AI
        </p>
      </div>

      {/* Search Form */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Search Query
            </label>
            <textarea
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Describe the symptoms or issue you're investigating..."
              className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent resize-none"
              rows={3}
              onKeyDown={(e) => {
                if (e.key === 'Enter' && (e.metaKey || e.ctrlKey)) {
                  handleSearch()
                }
              }}
            />
          </div>

          <div className="flex flex-wrap gap-4">
            <div className="flex-1 min-w-[200px]">
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Search Type
              </label>
              <select
                value={searchType}
                onChange={(e) => setSearchType(e.target.value as SearchType)}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
              >
                <option value="general">General Search</option>
                <option value="symptoms">Search by Symptoms</option>
                <option value="root-cause">Search by Root Cause</option>
              </select>
            </div>

            <div className="w-32">
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Top K Results
              </label>
              <input
                type="number"
                min="1"
                max="20"
                value={topK}
                onChange={(e) => setTopK(parseInt(e.target.value) || 5)}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
              />
            </div>
          </div>

          <button
            onClick={handleSearch}
            disabled={loading || !query.trim()}
            className="w-full bg-primary-600 text-white px-6 py-3 rounded-lg font-medium hover:bg-primary-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition-colors flex items-center justify-center space-x-2"
          >
            {loading ? (
              <>
                <Loader2 className="h-5 w-5 animate-spin" />
                <span>Searching...</span>
              </>
            ) : (
              <>
                <Search className="h-5 w-5" />
                <span>Search</span>
              </>
            )}
          </button>
        </div>
      </div>

      {/* Error Message */}
      {error && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 flex items-start space-x-3">
          <AlertCircle className="h-5 w-5 text-red-600 flex-shrink-0 mt-0.5" />
          <div>
            <h3 className="text-sm font-medium text-red-800">Error</h3>
            <p className="text-sm text-red-700 mt-1">{error}</p>
          </div>
        </div>
      )}

      {/* Results */}
      {results && (
        <div className="space-y-6">
          {/* Summary Card */}
          <div className="bg-gradient-to-r from-primary-50 to-primary-100 rounded-lg shadow-sm border border-primary-200 p-6">
            <div className="flex items-start justify-between">
              <div className="flex-1">
                <h3 className="text-lg font-semibold text-gray-900 mb-2">
                  Suggested Root Cause
                </h3>
                <p className="text-gray-700 leading-relaxed">{results.summary.suggestedRootCause}</p>
              </div>
              <div className="ml-4 flex flex-col items-end space-y-2">
                <span
                  className={`px-3 py-1 rounded-full text-sm font-medium ${getConfidenceColor(
                    results.summary.confidence
                  )}`}
                >
                  {results.summary.confidence} Confidence
                </span>
                <div className="text-sm text-gray-600">
                  <TrendingUp className="h-4 w-4 inline mr-1" />
                  {results.summary.similarIncidents} similar incidents
                </div>
                <div className="text-xs text-gray-500">
                  {results.executionTimeMs}ms
                </div>
              </div>
            </div>
          </div>

          {/* Results List */}
          <div>
            <h3 className="text-xl font-semibold text-gray-900 mb-4">
              Similar RCA Documents ({results.results.length})
            </h3>
            <div className="space-y-4">
              {results.results.map((result: SearchResult, index: number) => (
                <ResultCard key={result.pageId} result={result} index={index + 1} />
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

function ResultCard({ result, index }: { result: SearchResult; index: number }) {
  const similarityPercentage = Math.round(result.similarityScore * 100)

  return (
    <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6 hover:shadow-md transition-shadow">
      <div className="flex items-start justify-between mb-4">
        <div className="flex-1">
          <div className="flex items-center space-x-3 mb-2">
            <span className="flex items-center justify-center w-8 h-8 bg-primary-100 text-primary-700 rounded-full font-semibold text-sm">
              {index}
            </span>
            <h4 className="text-lg font-semibold text-gray-900">{result.title}</h4>
          </div>
          <div className="flex items-center space-x-4 text-sm text-gray-500 ml-11">
            <span className="flex items-center space-x-1">
              <span className="font-medium text-primary-600">{similarityPercentage}%</span>
              <span>similarity</span>
            </span>
            {result.incidentDate && (
              <span className="flex items-center space-x-1">
                <Calendar className="h-4 w-4" />
                <span>{format(new Date(result.incidentDate), 'MMM d, yyyy')}</span>
              </span>
            )}
          </div>
        </div>
        <a
          href={result.url}
          target="_blank"
          rel="noopener noreferrer"
          className="ml-4 p-2 text-gray-400 hover:text-primary-600 transition-colors"
          title="Open in Confluence"
        >
          <ExternalLink className="h-5 w-5" />
        </a>
      </div>

      <div className="ml-11 space-y-3">
        {result.symptoms && (
          <div>
            <h5 className="text-sm font-medium text-gray-700 mb-1">Symptoms:</h5>
            <p className="text-sm text-gray-600">{result.symptoms}</p>
          </div>
        )}
        {result.rootCause && (
          <div>
            <h5 className="text-sm font-medium text-gray-700 mb-1">Root Cause:</h5>
            <p className="text-sm text-gray-600">{result.rootCause}</p>
          </div>
        )}
        {result.resolution && (
          <div>
            <h5 className="text-sm font-medium text-gray-700 mb-1">Resolution:</h5>
            <p className="text-sm text-gray-600">{result.resolution}</p>
          </div>
        )}
      </div>
    </div>
  )
}

