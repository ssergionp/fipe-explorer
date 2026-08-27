import { Route, Routes } from 'react-router-dom'
import { Layout } from './Layout'
import { ComparePage } from './pages/ComparePage'
import { InsightsPage } from './pages/InsightsPage'
import { LoginPage } from './pages/LoginPage'
import { MyVehiclesPage } from './pages/MyVehiclesPage'
import { PrivacyPolicyPage } from './pages/PrivacyPolicyPage'
import { RegisterPage } from './pages/RegisterPage'
import { SearchPage } from './pages/SearchPage'
import { TermsOfUsePage } from './pages/TermsOfUsePage'
import { VehicleDetailPage } from './pages/VehicleDetailPage'

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<SearchPage />} />
        <Route path="/vehicles/:modelId" element={<VehicleDetailPage />} />
        <Route path="/compare" element={<ComparePage />} />
        <Route path="/insights" element={<InsightsPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/cadastro" element={<RegisterPage />} />
        <Route path="/privacidade" element={<PrivacyPolicyPage />} />
        <Route path="/termos" element={<TermsOfUsePage />} />
        <Route path="/meus-veiculos" element={<MyVehiclesPage />} />
      </Route>
    </Routes>
  )
}
