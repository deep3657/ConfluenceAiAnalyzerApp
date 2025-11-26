import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom'
import Layout from './components/Layout'
import SearchPage from './pages/SearchPage'
import IngestionPage from './pages/IngestionPage'
import StatsPage from './pages/StatsPage'

function App() {
  return (
    <Router>
      <Layout>
        <Routes>
          <Route path="/" element={<Navigate to="/search" replace />} />
          <Route path="/search" element={<SearchPage />} />
          <Route path="/ingestion" element={<IngestionPage />} />
          <Route path="/stats" element={<StatsPage />} />
        </Routes>
      </Layout>
    </Router>
  )
}

export default App

