import React, { useState, useEffect } from 'react';
import { Card, Form, Button, Alert } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import DashboardLayout from '../../components/layout/DashboardLayout';
import { useAuth } from '../../context/AuthContext';
import API from '../../api/axiosInstance';
import { toast } from 'react-toastify';

const MIN_FINDINGS = 20;
const MAX_FINDINGS = 200;

const ScheduleInspectionPage = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const [inspectionTypes, setInspectionTypes] = useState([]);
  const [formData, setFormData] = useState({ projectId: '', inspectionType: '', findings: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    API.get('/api/safety/inspections/types')
      .then((res) => {
        const types = res.data || [];
        setInspectionTypes(types);
        if (types.length > 0) setFormData((f) => ({ ...f, inspectionType: types[0] }));
      })
      .catch(() => toast.error('Failed to load inspection types'));
  }, []);

  // Unauthorised users — guard AFTER all hooks
  if (user?.role === 'PROJECT_MANAGER') {
    return (
      <DashboardLayout title="Schedule Inspection" onLogout={logout}>
        <Alert variant="danger" className="mt-2">
          You do not have permission to schedule inspections. Only Safety Officers and Admins can do this.
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
    if (!formData.inspectionType) return 'Inspection type is required.';
    if (formData.findings.trim().length < MIN_FINDINGS)
      return `Findings must be at least ${MIN_FINDINGS} characters.`;
    if (formData.findings.length > MAX_FINDINGS)
      return `Findings must be under ${MAX_FINDINGS} characters.`;
    return null;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const validationError = validate();
    if (validationError) { setError(validationError); return; }

    setLoading(true);
    try {
      const res = await API.post('/api/safety/inspections', formData);
      toast.success('Inspection scheduled successfully!');
      navigate(`/safety/inspections/${res.data.inspectionId}`);
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to schedule inspection';
      setError(msg);
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  };

  const charCount = formData.findings.length;
  const charWarning = charCount > MAX_FINDINGS * 0.9;
  const charTooShort = charCount > 0 && charCount < MIN_FINDINGS;

  return (
    <DashboardLayout title="Schedule Inspection" onLogout={logout}>
      <nav aria-label="breadcrumb">
        <ol className="breadcrumb bg-transparent p-0 mb-2">
          <li className="breadcrumb-item">
            <button className="btn btn-link p-0" onClick={() => navigate('/safety/inspections')}>Inspections</button>
          </li>
          <li className="breadcrumb-item active">Schedule New</li>
        </ol>
      </nav>

      <Card className="shadow-sm" style={{ maxWidth: 700 }}>
        <Card.Header>
          <h5 className="mb-0 heading-primary">Schedule Safety Inspection</h5>
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
              <Form.Text className="text-muted">Enter the ID of the project to inspect.</Form.Text>
            </Form.Group>

            <Form.Group className="mb-3">
              <Form.Label>Inspection Type <span className="text-danger">*</span></Form.Label>
              <Form.Select name="inspectionType" value={formData.inspectionType} onChange={handleChange}>
                {inspectionTypes.map((t) => (
                  <option key={t} value={t}>{t.replace(/_/g, ' ')}</option>
                ))}
              </Form.Select>
            </Form.Group>

            <Form.Group className="mb-3">
              <Form.Label>Findings / Notes <span className="text-danger">*</span></Form.Label>
              <Form.Control
                as="textarea"
                rows={4}
                name="findings"
                value={formData.findings}
                onChange={handleChange}
                placeholder="Describe the inspection findings or planned scope (20–200 characters)..."
                maxLength={MAX_FINDINGS}
                required
              />
              <Form.Text className={charWarning ? 'text-danger' : charTooShort ? 'text-warning' : 'text-muted'}>
                {charCount} / {MAX_FINDINGS} characters
                {charTooShort && ` — minimum ${MIN_FINDINGS} required`}
              </Form.Text>
            </Form.Group>

            <div className="d-flex gap-2">
              <Button type="submit" className="btn-brand" disabled={loading}>
                {loading ? 'Scheduling…' : 'Schedule Inspection'}
              </Button>
              <Button variant="outline-secondary" onClick={() => navigate('/safety/inspections')}>
                Cancel
              </Button>
            </div>
          </Form>
        </Card.Body>
      </Card>
    </DashboardLayout>
  );
};

export default ScheduleInspectionPage;
