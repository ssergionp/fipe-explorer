import { Route, Routes } from 'react-router-dom'
import { Layout } from './Layout'
import { ComparePage } from './pages/ComparePage'
import { InsightsPage } from './pages/InsightsPage'
import { SearchPage } from './pages/SearchPage'
import { VehicleDetailPage } from './pages/VehicleDetailPage'

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<SearchPage />} />
        <Route path="/vehicles/:modelId" element={<VehicleDetailPage />} />
        <Route path="/compare" element={<ComparePage />} />
        <Route path="/insights" element={<InsightsPage />} />
      </Route>
    </Routes>
  )
}
