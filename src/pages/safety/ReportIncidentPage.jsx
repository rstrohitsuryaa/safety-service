import React, { useState } from 'react';
import { Card, Form, Button, Alert } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import DashboardLayout from '../../components/layout/DashboardLayout';
import { useAuth } from '../../context/AuthContext';
import API from '../../api/axiosInstance';
import { toast } from 'react-toastify';

const MAX_DESC = 5000;

const ReportIncidentPage = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const [formData, setFormData] = useState({ projectId: '', description: '', severity: 'MEDIUM' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  // Unauthorised users see a permission message
  if (user?.role === 'PROJECT_MANAGER') {
    return (
      <DashboardLayout title="Report Incident" onLogout={logout}>
        <Alert variant="danger" className="mt-2">
          You do not have permission to report incidents. Only Safety Officers and Admins can do this.
        </Alert>
      </DashboardLayout>
    );
  }

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
    setError('');
  };

  const validate = () => {
    if (!formData.projectId.trim()) return 'Project ID is required.';
    if (!formData.description.trim()) return 'Description is required.';
    if (formData.description.length > MAX_DESC) return `Description must be under ${MAX_DESC} characters.`;
    return null;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const validationError = validate();
    if (validationError) { setError(validationError); return; }

    setLoading(true);
    try {
      const res = await API.post('/api/safety/incidents', formData);
      toast.success('Incident reported successfully!');
      navigate(`/safety/incidents/${res.data.incidentId}`);
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to report incident';
      setError(msg);
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  };

  const charCount = formData.description.length;
  const charWarning = charCount > MAX_DESC * 0.9;

  return (
    <DashboardLayout title="Report Incident" onLogout={logout}>
      <nav aria-label="breadcrumb">
        <ol className="breadcrumb bg-transparent p-0 mb-2">
          <li className="breadcrumb-item">
            <button className="btn btn-link p-0" onClick={() => navigate('/safety/incidents')}>Incidents</button>
          </li>
          <li className="breadcrumb-item active">Report New</li>
        </ol>
      </nav>

      <Card className="shadow-sm" style={{ maxWidth: 700 }}>
        <Card.Header>
          <h5 className="mb-0 heading-primary">Report Safety Incident</h5>
        </Card.Header>
        <Card.Body className="p-4">
          {error && <Alert variant="danger">{error}</Alert>}

          <Form onSubmit={handleSubmit}>
            <Form.Group className="mb-3">
              <Form.Label>Project ID <span className="text-danger">*</span></Form.Label>
              <Form.Control
                name="projectId"
                value={formData.projectId}
                onChange={handleChange}
                placeholder="Enter the project ID"
                required
              />
              <Form.Text className="text-muted">Enter the ID of the project where the incident occurred.</Form.Text>
            </Form.Group>

            <Form.Group className="mb-3">
              <Form.Label>Severity <span className="text-danger">*</span></Form.Label>
              <Form.Select name="severity" value={formData.severity} onChange={handleChange}>
                <option value="LOW">LOW — Minor risk, no immediate danger</option>
                <option value="MEDIUM">MEDIUM — Moderate risk</option>
                <option value="HIGH">HIGH — Significant danger present</option>
                <option value="CRITICAL">CRITICAL — Life-threatening, immediate action required</option>
              </Form.Select>
            </Form.Group>

            <Form.Group className="mb-3">
              <Form.Label>Description <span className="text-danger">*</span></Form.Label>
              <Form.Control
                as="textarea"
                rows={6}
                name="description"
                value={formData.description}
                onChange={handleChange}
                placeholder="Describe what happened, where, and what actions were taken..."
                maxLength={MAX_DESC}
                required
              />
              <Form.Text className={charWarning ? 'text-danger' : 'text-muted'}>
                {charCount} / {MAX_DESC} characters
              </Form.Text>
            </Form.Group>

            <div className="d-flex gap-2">
              <Button type="submit" className="btn-brand" disabled={loading}>
                {loading ? 'Submitting…' : 'Report Incident'}
              </Button>
              <Button variant="outline-secondary" onClick={() => navigate('/safety/incidents')}>
                Cancel
              </Button>
            </div>
          </Form>
        </Card.Body>
      </Card>
    </DashboardLayout>
  );
};

export default ReportIncidentPage;
