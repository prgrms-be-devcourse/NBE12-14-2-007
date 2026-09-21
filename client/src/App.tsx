import { Router } from '@/app/router'
import { AuthProvider } from '@/features/auth/AuthProvider'
import { BrowserRouter } from 'react-router'

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Router />
      </AuthProvider>
    </BrowserRouter>
  )
}
