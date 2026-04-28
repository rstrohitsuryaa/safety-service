import React from 'react';
import { Badge } from 'react-bootstrap';

const SEVERITY_STYLES = {
  LOW:      { bg: 'secondary', style: {} },
  MEDIUM:   { bg: 'warning',   style: { color: '#000' } },
  HIGH:     { bg: null,        style: { backgroundColor: '#fd7e14', color: '#fff' } },
  CRITICAL: { bg: 'danger',    style: {} },
};

const SeverityBadge = ({ severity }) => {
  const conf = SEVERITY_STYLES[severity] || { bg: 'secondary', style: {} };
  return conf.bg
    ? <Badge bg={conf.bg} style={conf.style}>{severity}</Badge>
    : <Badge style={conf.style}>{severity}</Badge>;
};

export default SeverityBadge;
