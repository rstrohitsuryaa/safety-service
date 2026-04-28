import React from 'react';
import { Table, Button } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import SeverityBadge from './SeverityBadge';
import StatusBadge from './StatusBadge';

const IncidentTable = ({ incidents = [] }) => {
  const navigate = useNavigate();

  if (incidents.length === 0) {
    return <p className="text-muted text-center py-4 mb-0">No incidents found.</p>;
  }

  return (
    <Table responsive hover className="align-middle mb-0">
      <thead className="table-header">
        <tr>
          <th>ID</th>
          <th>Project</th>
          <th>Date</th>
          <th>Description</th>
          <th>Severity</th>
          <th>Status</th>
          <th>Reported By</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        {incidents.map((inc) => (
          <tr key={inc.incidentId}>
            <td><code>{inc.incidentId?.slice(0, 8)}…</code></td>
            <td>{inc.projectId}</td>
            <td>{inc.date}</td>
            <td className="text-truncate" style={{ maxWidth: 200 }}>{inc.description}</td>
            <td><SeverityBadge severity={inc.severity} /></td>
            <td><StatusBadge status={inc.status} /></td>
            <td>{inc.reportedByName || inc.reportedBy}</td>
            <td>
              <Button
                size="sm"
                variant="outline-primary"
                onClick={() => navigate(`/safety/incidents/${inc.incidentId}`)}
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

export default IncidentTable;
