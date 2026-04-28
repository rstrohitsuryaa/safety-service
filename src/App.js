import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ToastContainer } from 'react-toastify';
import { AuthProvider } from './context/AuthContext';
import AppNavbar from './components/common/AppNavbar';
import ProtectedRoute from './components/common/ProtectedRoute';
import AdminRoute from './components/common/AdminRoute';
import LandingPage from './pages/LandingPage';
import LoginPage from './pages/LoginPage';
import SignupPage from './pages/SignupPage';
import AuthPortal from './pages/AuthPortal';
import ForgotPasswordPage from './pages/ForgotPasswordPage';
import ResetPasswordPage from './pages/ResetPasswordPage';
import DashboardPage from './pages/DashboardPage';
import ProfilePage from './pages/ProfilePage';
import UserManagementPage from './pages/admin/UserManagementPage';
import PendingApprovalsPage from './pages/admin/PendingApprovalsPage';
import AuditLogsPage from './pages/admin/AuditLogsPage';
import SafetyDashboardPage from './pages/safety/SafetyDashboardPage';
import IncidentsPage from './pages/safety/IncidentsPage';
import IncidentDetailPage from './pages/safety/IncidentDetailPage';
import ReportIncidentPage from './pages/safety/ReportIncidentPage';
import InspectionsPage from './pages/safety/InspectionsPage';
import InspectionDetailPage from './pages/safety/InspectionDetailPage';
import ScheduleInspectionPage from './pages/safety/ScheduleInspectionPage';
import NotificationsPage from './pages/safety/NotificationsPage';
import 'bootstrap/dist/css/bootstrap.min.css';
import 'react-toastify/dist/ReactToastify.css';
import './App.css';

function App() {
  return (
    <Router>
      <AuthProvider>
        <AppNavbar />
        <Routes>
          <Route path="/login" element={<AuthPortal initial="signin" />} />
          <Route path="/signup" element={<AuthPortal initial="signup" />} />
          <Route path="/" element={<LandingPage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          <Route path="/reset-password" element={<ResetPasswordPage />} />
          <Route path="/dashboard" element={<ProtectedRoute><DashboardPage /></ProtectedRoute>} />
          <Route path="/profile" element={<ProtectedRoute><ProfilePage /></ProtectedRoute>} />
          <Route path="/admin/users" element={<AdminRoute><UserManagementPage /></AdminRoute>} />
          <Route path="/admin/pending" element={<AdminRoute><PendingApprovalsPage /></AdminRoute>} />
          <Route path="/admin/audit" element={<AdminRoute><AuditLogsPage /></AdminRoute>} />
          {/* Safety Module */}
          <Route path="/safety/dashboard"      element={<ProtectedRoute><SafetyDashboardPage /></ProtectedRoute>} />
          <Route path="/safety/incidents"      element={<ProtectedRoute><IncidentsPage /></ProtectedRoute>} />
          <Route path="/safety/incidents/new"  element={<ProtectedRoute><ReportIncidentPage /></ProtectedRoute>} />
          <Route path="/safety/incidents/:id"  element={<ProtectedRoute><IncidentDetailPage /></ProtectedRoute>} />
          <Route path="/safety/inspections"    element={<ProtectedRoute><InspectionsPage /></ProtectedRoute>} />
          <Route path="/safety/inspections/new" element={<ProtectedRoute><ScheduleInspectionPage /></ProtectedRoute>} />
          <Route path="/safety/inspections/:id" element={<ProtectedRoute><InspectionDetailPage /></ProtectedRoute>} />
          <Route path="/safety/notifications"  element={<ProtectedRoute><NotificationsPage /></ProtectedRoute>} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
        <ToastContainer position="top-right" autoClose={3000} hideProgressBar={false} />
      </AuthProvider>
    </Router>
  );
}

export default App;
