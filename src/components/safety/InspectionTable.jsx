import React from 'react';
import { Table, Button } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import StatusBadge from './StatusBadge';

const InspectionTable = ({ inspections = [] }) => {
  const navigate = useNavigate();

  if (inspections.length === 0) {
    return <p className="text-muted text-center py-4 mb-0">No inspections found.</p>;
  }

  return (
    <Table responsive hover className="align-middle mb-0">
      <thead className="table-header">
        <tr>
          <th>ID</th>
          <th>Project</th>
          <th>Date</th>
          <th>Type</th>
          <th>Officer</th>
          <th>Findings</th>
          <th>Status</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        {inspections.map((ins) => (
          <tr key={ins.inspectionId}>
            <td><code>{ins.inspectionId?.slice(0, 8)}…</code></td>
            <td>{ins.projectId}</td>
            <td>{ins.date}</td>
            <td><small>{ins.inspectionType?.replace(/_/g, ' ')}</small></td>
            <td>{ins.officerName || ins.officerId}</td>
            <td className="text-truncate" style={{ maxWidth: 180 }}>{ins.findings}</td>
            <td><StatusBadge status={ins.status} /></td>
            <td>
              <Button
                size="sm"
                variant="outline-primary"
                onClick={() => navigate(`/safety/inspections/${ins.inspectionId}`)}
              >
                View
              </Button>
            </td>
          </tr>
        ))}
      </tbody>
    </Table>
  );
};

export default InspectionTable;
