import React from 'react';
import { Badge } from 'react-bootstrap';

const STATUS_MAP = {
  // Incident statuses
  OPEN:                 { bg: 'primary',   style: {} },
  UNDER_INVESTIGATION:  { bg: null,        style: { backgroundColor: '#6f42c1', color: '#fff' } },
  RESOLVED:             { bg: 'success',   style: {} },
  // Inspection statuses
  SCHEDULED:            { bg: 'primary',   style: {} },
  IN_PROGRESS:          { bg: 'warning',   style: { color: '#000' } },
  COMPLETED:            { bg: 'success',   style: {} },
  NON_COMPLIANT:        { bg: 'danger',    style: {} },
  // Shared
  CLOSED:               { bg: 'secondary', style: {} },
};

const LABELS = {
  UNDER_INVESTIGATION: 'Under Investigation',
  IN_PROGRESS:         'In Progress',
  NON_COMPLIANT:       'Non Compliant',
};

const StatusBadge = ({ status }) => {
  const conf = STATUS_MAP[status] || { bg: 'secondary', style: {} };
  const label = LABELS[status] || (status ? status.replace(/_/g, ' ') : '—');
  return conf.bg
    ? <Badge bg={conf.bg} style={conf.style}>{label}</Badge>
    : <Badge style={conf.style}>{label}</Badge>;
};

export default StatusBadge;
