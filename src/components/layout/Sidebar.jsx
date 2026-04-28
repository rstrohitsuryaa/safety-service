import React from 'react';
import { Nav } from 'react-bootstrap';
import { NavLink } from 'react-router-dom';
import {
  FaTachometerAlt, FaUsers, FaClipboardList, FaFileAlt,
  FaShieldAlt, FaExclamationTriangle, FaClipboardCheck, FaBell,
} from 'react-icons/fa';
import { useAuth } from '../../context/AuthContext';

const ALL_ROLES = ['ADMIN', 'PROJECT_MANAGER', 'SITE_ENGINEER', 'SAFETY_OFFICER', 'VENDOR', 'FINANCE_OFFICER', 'GUEST'];
const SAFETY_ROLES = ['ADMIN', 'SAFETY_OFFICER', 'PROJECT_MANAGER'];

const Sidebar = () => {
  const { user } = useAuth();
  const role = user?.role || 'GUEST';

  // Sidebar items with RBAC rules
  const items = [
    // ── Core ──────────────────────────────────────────────────────
    { key: 'dashboard',        label: 'Dashboard',          to: '/dashboard',              icon: <FaTachometerAlt className="me-2" />,     roles: ALL_ROLES },
    { key: 'users',            label: 'User Management',    to: '/admin/users',            icon: <FaUsers className="me-2" />,             roles: ['ADMIN'] },
    { key: 'pending',          label: 'Pending Approvals',  to: '/admin/pending',          icon: <FaClipboardList className="me-2" />,     roles: ['ADMIN', 'PROJECT_MANAGER'] },
    { key: 'audit',            label: 'Audit Logs',         to: '/admin/audit',            icon: <FaFileAlt className="me-2" />,           roles: ['ADMIN'] },
    // ── Safety Module ─────────────────────────────────────────────
    { key: 'safety-dashboard', label: 'Safety Dashboard',   to: '/safety/dashboard',       icon: <FaShieldAlt className="me-2" />,         roles: SAFETY_ROLES },
    { key: 'incidents',        label: 'Incidents',          to: '/safety/incidents',       icon: <FaExclamationTriangle className="me-2" />,roles: SAFETY_ROLES },
    { key: 'inspections',      label: 'Inspections',        to: '/safety/inspections',     icon: <FaClipboardCheck className="me-2" />,    roles: SAFETY_ROLES },
    { key: 'notifications',    label: 'Notifications',      to: '/safety/notifications',   icon: <FaBell className="me-2" />,              roles: SAFETY_ROLES },
  ];

  // Filter items based on role
  const visibleItems = items.filter((item) => item.roles.includes(role));
  const prettyRole = (r) => (r ? r.replace(/_/g, ' ') : 'User');

  // Split items into groups for visual dividers
  const coreItems   = visibleItems.filter((i) => ['dashboard','users','pending','audit'].includes(i.key));
  const safetyItems = visibleItems.filter((i) => ['safety-dashboard','incidents','inspections','notifications'].includes(i.key));

  const renderItems = (list) =>
    list.map((item) => (
      <Nav.Item key={item.key}>
        <NavLink
          to={item.to}
          className={({ isActive }) => isActive ? 'nav-link active text-white' : 'nav-link text-white'}
        >
          {item.icon} {item.label}
        </NavLink>
      </Nav.Item>
    ));

  return (
    <aside className="sidebar d-flex flex-column p-3 text-white" id="app-sidebar">
      <div className="brand mb-4 d-flex align-items-center">
        <div className="brand-logo me-2">
          <span className="logo-mark">BS</span>
        </div>
        <div>
          <div className="h5 mb-0 brand-name"><span className="brand-build">Build</span>Smart</div>
          <small className="text-muted">{prettyRole(role)}</small>
        </div>
      </div>

      <Nav className="flex-column" variant="pills">
        {renderItems(coreItems)}
        {safetyItems.length > 0 && (
          <>
            <hr className="border-secondary my-2" />
            <small className="text-muted px-2 mb-1" style={{ fontSize: '0.7rem', textTransform: 'uppercase', letterSpacing: '0.8px' }}>
              Safety
            </small>
            {renderItems(safetyItems)}
          </>
        )}
      </Nav>

      <div className="mt-auto small text-muted">© {new Date().getFullYear()} BuildSmart</div>
    </aside>
  );
};

export default Sidebar;
