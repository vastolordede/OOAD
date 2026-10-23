import { BrowserRouter } from 'react-router-dom'
import { ToastProvider } from './components/common/ToastProvider'
import { AuthProvider } from './contexts/AuthContext'
import { AppRoutes } from './routes/AppRoutes'
import './App.css'

function App() {
  return (
    <AuthProvider>
      <ToastProvider>
        <BrowserRouter>
          <AppRoutes />
        </BrowserRouter>
      </ToastProvider>
    </AuthProvider>
  )
}

export default App
