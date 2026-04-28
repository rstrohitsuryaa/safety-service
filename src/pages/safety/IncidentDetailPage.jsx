import React, { useState, useEffect } from 'react';
import { Card, Row, Col, Button, Form, Alert } from 'react-bootstrap';
import { useParams, useNavigate } from 'react-router-dom';
import DashboardLayout from '../../components/layout/DashboardLayout';
import { useAuth } from '../../context/AuthContext';
import API from '../../api/axiosInstance';
import SeverityBadge from '../../components/safety/SeverityBadge';
import StatusBadge from '../../components/safety/StatusBadge';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import ConfirmModal from '../../components/common/ConfirmModal';
import { toast } from 'react-toastify';

const INCIDENT_TRANSITIONS = {
  OPEN:                ['UNDER_INVESTIGATION'],
  UNDER_INVESTIGATION: ['RESOLVED'],
  RESOLVED:            ['CLOSED'],
  CLOSED:              [],
};

const IncidentDetailPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const canWrite = user?.role === 'SAFETY_OFFICER' || user?.role === 'ADMIN';

  const [incident, setIncident] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [nextStatus, setNextStatus] = useState('');
  const [updatingStatus, setUpdatingStatus] = useState(false);
  const [showDeleteModal, setShowDeleteModal] = useState(false);

  useEffect(() => { fetchIncident(); }, [id]);

  const fetchIncident = async () => {
    setLoading(true);
    try {
      const res = await API.get(`/api/safety/incidents/${id}`);
      setIncident(res.data);
      const transitions = INCIDENT_TRANSITIONS[res.data.status] || [];
      setNextStatus(transitions[0] || '');
    } catch (err) {
      setError(err.response?.status === 404 ? 'Incident not found.' : 'Failed to load incident.');
    } finally {
      setLoading(false);
    }
  };

  const handleUpdateStatus = async () => {
    if (!nextStatus) return;
    setUpdatingStatus(true);
    try {
      const res = await API.patch(`/api/safety/incidents/${id}/status?status=${nextStatus}`);
      setIncident(res.data);
      const transitions = INCIDENT_TRANSITIONS[res.data.status] || [];
      setNextStatus(transitions[0] || '');
      toast.success(`Status updated to ${nextStatus.replace(/_/g, ' ')}`);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to update status');
    } finally {
      setUpdatingStatus(false);
    }
  };

  const handleDelete = async () => {
    try {
      await API.delete(`/api/safety/incidents/${id}`);
      toast.success('Incident deleted successfully');
      navigate('/safety/incidents');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to delete incident');
      setShowDeleteModal(false);
    }
  };

  if (loading) return <DashboardLayout title="Incident Detail" onLogout={logout}><LoadingSpinner /></DashboardLayout>;

  if (error) {
    return (
      <DashboardLayout title="Incident Detail" onLogout={logout}>
        <Alert variant="danger">{error}</Alert>
        <Button variant="outline-secondary" onClick={() => navigate('/safety/incidents')}>← Back to Incidents</Button>
      </DashboardLayout>
    );
  }

  const validTransitions = INCIDENT_TRANSITIONS[incident.status] || [];

  return (
    <DashboardLayout title="Incident Detail" onLogout={logout}>
      {/* Breadcrumb */}
      <nav aria-label="breadcrumb">
        <ol className="breadcrumb bg-transparent p-0 mb-2">
          <li className="breadcrumb-item">
            <button className="btn btn-link p-0" onClick={() => navigate('/safety/incidents')}>Incidents</button>
          </li>
          <li className="breadcrumb-item active">Detail</li>
        </ol>
      </nav>

      <Card className="shadow-sm">
        <Card.Header className="d-flex justify-content-between align-items-center">
          <h5 className="mb-0 heading-primary">Incident Report</h5>
          <div className="d-flex gap-2 align-items-center">
            <SeverityBadge severity={incident.severity} />
            <StatusBadge status={incident.status} />
          </div>
        </Card.Header>

        <Card.Body className="p-4">
          <Row>
            <Col md={6} className="mb-3">
              <small className="text-muted d-block">Incident ID</small>
              <code className="fw-bold">{incident.incidentId}</code>
            </Col>
            <Col md={6} className="mb-3">
              <small className="text-muted d-block">Project ID</small>
              <p className="fw-bold mb-0">{incident.projectId}</p>
            </Col>
            <Col md={6} className="mb-3">
              <small className="text-muted d-block">Date</small>
              <p className="fw-bold mb-0">{incident.date}</p>
            </Col>
            <Col md={6} className="mb-3">
              <small className="text-muted d-block">Reported By</small>
              <p className="fw-bold mb-0">{incident.reportedByName || incident.reportedBy}</p>
            </Col>
            <Col md={12} className="mb-3">
              <small className="text-muted d-block">Description</small>
              <p className="mb-0" style={{ whiteSpace: 'pre-wrap' }}>{incident.description}</p>
            </Col>
          </Row>

          {/* Actions for SAFETY_OFFICER / ADMIN */}
          {canWrite && (
            <div className="mt-3 pt-3 border-top d-flex flex-wrap gap-3 align-items-center">
              {validTransitions.length > 0 && (
                <div className="d-flex align-items-center gap-2">
                  <Form.Select
                    size="sm"
                    value={nextStatus}
                    onChange={(e) => setNextStatus(e.target.value)}
                    style={{ width: 220 }}
                  >
                    {validTransitions.map((s) => (
                      <option key={s} value={s}>{s.replace(/_/g, ' ')}</option>
                    ))}
                  </Form.Select>
                  <Button size="sm" className="btn-brand" onClick={handleUpdateStatus} disabled={updatingStatus}>
                    {updatingStatus ? 'Updating…' : 'Update Status'}
                  </Button>
                </div>
              )}
              {incident.status === 'OPEN' && (
                <Button size="sm" variant="outline-danger" onClick={() => setShowDeleteModal(true)}>
                  Delete Incident
                </Button>
              )}
              {validTransitions.length === 0 && incident.status === 'CLOSED' && (
                <span className="text-muted small">This incident is closed — no further transitions available.</span>
              )}
            </div>
          )}
        </Card.Body>
      </Card>

      <ConfirmModal
        show={showDeleteModal}
        onHide={() => setShowDeleteModal(false)}
        onConfirm={handleDelete}
        title="Delete Incident"
        message="Are you sure you want to delete this incident? This action cannot be undone."
        confirmText="Delete"
        variant="danger"
      />
    </DashboardLayout>
  );
};

export default IncidentDetailPage;
