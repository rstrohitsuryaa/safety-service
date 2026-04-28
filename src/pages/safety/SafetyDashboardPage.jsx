import React, { useState, useEffect } from 'react';
import { Row, Col, Card, Table, Button } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import DashboardLayout from '../../components/layout/DashboardLayout';
import { useAuth } from '../../context/AuthContext';
import API from '../../api/axiosInstance';
import SeverityBadge from '../../components/safety/SeverityBadge';
import StatusBadge from '../../components/safety/StatusBadge';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import { toast } from 'react-toastify';

const SafetyDashboardPage = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const canWrite = user?.role === 'SAFETY_OFFICER' || user?.role === 'ADMIN';

  const [stats, setStats] = useState({
    openIncidents: 0,
    criticalIncidents: 0,
    scheduledInspections: 0,
    unreadNotifications: 0,
  });
  const [recentIncidents, setRecentIncidents] = useState([]);
  const [recentInspections, setRecentInspections] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => { fetchAll(); }, []);

  const fetchAll = async () => {
    try {
      const [openInc, critInc, schedIns, unread, recentInc, recentIns] =
        await Promise.allSettled([
          API.get('/api/safety/incidents?status=OPEN&page=0&size=1'),
          API.get('/api/safety/incidents?severity=CRITICAL&status=OPEN&page=0&size=1'),
          API.get('/api/safety/inspections?status=SCHEDULED&page=0&size=1'),
          API.get(`/api/safety/notifications/unread-count/${user?.userId}`),
          API.get('/api/safety/incidents?page=0&size=5'),
          API.get('/api/safety/inspections?page=0&size=5'),
        ]);

      setStats({
        openIncidents:       openInc.status  === 'fulfilled' ? (openInc.value.data.totalElements  ?? 0) : 0,
        criticalIncidents:   critInc.status  === 'fulfilled' ? (critInc.value.data.totalElements  ?? 0) : 0,
        scheduledInspections:schedIns.status === 'fulfilled' ? (schedIns.value.data.totalElements ?? 0) : 0,
        unreadNotifications: unread.status   === 'fulfilled' ? (unread.value.data.unreadCount     ?? 0) : 0,
      });
      setRecentIncidents(   recentInc.status === 'fulfilled' ? (recentInc.value.data.content  || []) : []);
      setRecentInspections( recentIns.status === 'fulfilled' ? (recentIns.value.data.content  || []) : []);
    } catch {
      toast.error('Failed to load safety dashboard data');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <DashboardLayout title="Safety Dashboard" onLogout={logout}>
        <LoadingSpinner message="Loading safety dashboard..." />
      </DashboardLayout>
    );
  }

  const statCards = [
    { label: 'Open Incidents',         value: stats.openIncidents,        icon: '⚠️',  color: '#dc3545', path: '/safety/incidents?status=OPEN' },
    { label: 'Critical Incidents',     value: stats.criticalIncidents,    icon: '🔴',  color: '#fd7e14', path: '/safety/incidents?severity=CRITICAL' },
    { label: 'Scheduled Inspections',  value: stats.scheduledInspections, icon: '📋',  color: '#0b64d6', path: '/safety/inspections?status=SCHEDULED' },
    { label: 'Unread Notifications',   value: stats.unreadNotifications,  icon: '🔔',  color: '#6f42c1', path: '/safety/notifications' },
  ];

  return (
    <DashboardLayout title="Safety Dashboard" onLogout={logout}>
      <nav aria-label="breadcrumb">
        <ol className="breadcrumb bg-transparent p-0 mb-2">
          <li className="breadcrumb-item active">Safety Dashboard</li>
        </ol>
      </nav>
      <h2 className="mb-1">Safety Overview</h2>
      <p className="text-muted mb-3">Monitor incidents, inspections, and notifications.</p>

      {/* Stat Cards */}
      <Row className="g-2 mb-3">
        {statCards.map((s) => (
          <Col md={6} lg={3} key={s.label}>
            <Card
              className="stat-card shadow-sm h-100"
              style={{ cursor: 'pointer' }}
              onClick={() => navigate(s.path)}
            >
              <Card.Body className="d-flex align-items-center">
                <div className="me-3 display-6 text-muted">{s.icon}</div>
                <div>
                  <div className="text-muted small">{s.label}</div>
                  <div className="h5 mb-0" style={{ color: s.color }}>{s.value}</div>
                </div>
              </Card.Body>
            </Card>
          </Col>
        ))}
      </Row>

      {/* Quick Actions */}
      {canWrite && (
        <div className="mb-3 d-flex gap-2 flex-wrap">
          <Button className="btn-brand" onClick={() => navigate('/safety/incidents/new')}>
            + Report Incident
          </Button>
          <Button variant="outline-primary" onClick={() => navigate('/safety/inspections/new')}>
            + Schedule Inspection
          </Button>
        </div>
      )}

      <Row>
        {/* Recent Incidents */}
        <Col lg={6}>
          <Card className="shadow-sm">
            <Card.Header className="d-flex justify-content-between align-items-center">
              <strong>Recent Incidents</strong>
              <Button size="sm" variant="link" className="p-0" onClick={() => navigate('/safety/incidents')}>
                View all →
              </Button>
            </Card.Header>
            <Card.Body className="p-0">
              <Table hover responsive size="sm" className="mb-0 align-middle">
                <thead className="table-header">
                  <tr><th>Date</th><th>Severity</th><th>Status</th><th></th></tr>
                </thead>
                <tbody>
                  {recentIncidents.length === 0 ? (
                    <tr><td colSpan="4" className="text-center text-muted py-3">No incidents</td></tr>
                  ) : recentIncidents.map((inc) => (
                    <tr key={inc.incidentId}>
                      <td>{inc.date}</td>
                      <td><SeverityBadge severity={inc.severity} /></td>
                      <td><StatusBadge status={inc.status} /></td>
                      <td>
                        <Button
                          size="sm" variant="link" className="p-0"
                          onClick={() => navigate(`/safety/incidents/${inc.incidentId}`)}
                        >
                          View
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </Table>
            </Card.Body>
          </Card>
        </Col>

        {/* Recent Inspections */}
        <Col lg={6}>
          <Card className="shadow-sm">
            <Card.Header className="d-flex justify-content-between align-items-center">
              <strong>Recent Inspections</strong>
              <Button size="sm" variant="link" className="p-0" onClick={() => navigate('/safety/inspections')}>
                View all →
              </Button>
            </Card.Header>
            <Card.Body className="p-0">
              <Table hover responsive size="sm" className="mb-0 align-middle">
                <thead className="table-header">
                  <tr><th>Date</th><th>Type</th><th>Status</th><th></th></tr>
                </thead>
                <tbody>
                  {recentInspections.length === 0 ? (
                    <tr><td colSpan="4" className="text-center text-muted py-3">No inspections</td></tr>
                  ) : recentInspections.map((ins) => (
                    <tr key={ins.inspectionId}>
                      <td>{ins.date}</td>
                      <td><small>{ins.inspectionType?.replace(/_/g, ' ')}</small></td>
                      <td><StatusBadge status={ins.status} /></td>
                      <td>
                        <Button
                          size="sm" variant="link" className="p-0"
                          onClick={() => navigate(`/safety/inspections/${ins.inspectionId}`)}
                        >
                          View
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </Table>
            </Card.Body>
          </Card>
        </Col>
      </Row>
    </DashboardLayout>
  );
};

export default SafetyDashboardPage;
