import React, { useState, useEffect, useCallback } from 'react';
import { Card, Row, Col, Form, Button } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import DashboardLayout from '../../components/layout/DashboardLayout';
import { useAuth } from '../../context/AuthContext';
import API from '../../api/axiosInstance';
import InspectionTable from '../../components/safety/InspectionTable';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import { toast } from 'react-toastify';

const EMPTY_FILTERS = { projectId: '', status: '', inspectionType: '', dateFrom: '', dateTo: '' };

const InspectionsPage = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const canWrite = user?.role === 'SAFETY_OFFICER' || user?.role === 'ADMIN';

  const [inspections, setInspections] = useState([]);
  const [inspectionTypes, setInspectionTypes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [filters, setFilters] = useState(EMPTY_FILTERS);
  const [appliedFilters, setAppliedFilters] = useState(EMPTY_FILTERS);

  // Load inspection types once on mount
  useEffect(() => {
    API.get('/api/safety/inspections/types')
      .then((res) => setInspectionTypes(res.data || []))
      .catch(() => {});
  }, []);

  const fetchInspections = useCallback(async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams({ page, size: 10 });
      if (appliedFilters.projectId)     params.append('projectId',     appliedFilters.projectId);
      if (appliedFilters.status)        params.append('status',        appliedFilters.status);
      if (appliedFilters.inspectionType)params.append('inspectionType',appliedFilters.inspectionType);
      if (appliedFilters.dateFrom)      params.append('dateFrom',      appliedFilters.dateFrom);
      if (appliedFilters.dateTo)        params.append('dateTo',        appliedFilters.dateTo);

      const res = await API.get(`/api/safety/inspections?${params}`);
      setInspections(res.data.content || []);
      setTotalPages(res.data.totalPages || 1);
      setTotalElements(res.data.totalElements || 0);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to load inspections');
    } finally {
      setLoading(false);
    }
  }, [page, appliedFilters]);

  useEffect(() => { fetchInspections(); }, [fetchInspections]);

  const handleFilterChange = (e) => setFilters({ ...filters, [e.target.name]: e.target.value });

  const handleApply = () => {
    setPage(0);
    setAppliedFilters({ ...filters });
  };

  const handleClear = () => {
    setFilters(EMPTY_FILTERS);
    setAppliedFilters(EMPTY_FILTERS);
    setPage(0);
  };

  return (
    <DashboardLayout title="Inspections" onLogout={logout}>
      <nav aria-label="breadcrumb">
        <ol className="breadcrumb bg-transparent p-0 mb-2">
          <li className="breadcrumb-item active">Inspections</li>
        </ol>
      </nav>

      <div className="d-flex justify-content-between align-items-center mb-3">
        <h5 className="heading-primary mb-0">
          Safety Inspections <span className="text-muted fw-normal fs-6">({totalElements} total)</span>
        </h5>
        {canWrite && (
          <Button className="btn-brand" onClick={() => navigate('/safety/inspections/new')}>
            + Schedule Inspection
          </Button>
        )}
      </div>

      {/* Filter Bar */}
      <Card className="shadow-sm mb-3">
        <Card.Body className="py-2">
          <Row className="g-2 align-items-end">
            <Col md={2}>
              <Form.Label className="small mb-1">Project ID</Form.Label>
              <Form.Control size="sm" name="projectId" value={filters.projectId} onChange={handleFilterChange} placeholder="Project ID" />
            </Col>
            <Col md={2}>
              <Form.Label className="small mb-1">Status</Form.Label>
              <Form.Select size="sm" name="status" value={filters.status} onChange={handleFilterChange}>
                <option value="">All Statuses</option>
                <option>SCHEDULED</option>
                <option>IN_PROGRESS</option>
                <option>COMPLETED</option>
                <option>NON_COMPLIANT</option>
                <option>CLOSED</option>
              </Form.Select>
            </Col>
            <Col md={2}>
              <Form.Label className="small mb-1">Type</Form.Label>
              <Form.Select size="sm" name="inspectionType" value={filters.inspectionType} onChange={handleFilterChange}>
                <option value="">All Types</option>
                {inspectionTypes.map((t) => (
                  <option key={t} value={t}>{t.replace(/_/g, ' ')}</option>
                ))}
              </Form.Select>
            </Col>
            <Col md={2}>
              <Form.Label className="small mb-1">Date From</Form.Label>
              <Form.Control size="sm" type="date" name="dateFrom" value={filters.dateFrom} onChange={handleFilterChange} />
            </Col>
            <Col md={2}>
              <Form.Label className="small mb-1">Date To</Form.Label>
              <Form.Control size="sm" type="date" name="dateTo" value={filters.dateTo} onChange={handleFilterChange} />
            </Col>
            <Col md={2} className="d-flex gap-2">
              <Button size="sm" className="btn-brand w-100" onClick={handleApply}>Filter</Button>
              <Button size="sm" variant="outline-secondary" className="w-100" onClick={handleClear}>Clear</Button>
            </Col>
          </Row>
        </Card.Body>
      </Card>

      {/* Results */}
      <Card className="shadow-sm">
        <Card.Body className="p-0">
          {loading ? (
            <LoadingSpinner message="Loading inspections..." />
          ) : (
            <InspectionTable inspections={inspections} />
          )}
        </Card.Body>
      </Card>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="d-flex justify-content-center gap-2 mt-3">
          <Button size="sm" variant="outline-primary" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
            Previous
          </Button>
          <span className="align-self-center text-muted">Page {page + 1} of {totalPages}</span>
          <Button size="sm" variant="outline-primary" disabled={page >= totalPages - 1} onClick={() => setPage((p) => p + 1)}>
            Next
          </Button>
        </div>
      )}
    </DashboardLayout>
  );
};

export default InspectionsPage;
