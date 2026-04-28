import React from 'react';
import { ListGroup, Badge } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';

dayjs.extend(relativeTime);

const TYPE_LINK = {
  INCIDENT_REPORTED:         (id) => `/safety/incidents/${id}`,
  INCIDENT_STATUS_CHANGED:   (id) => `/safety/incidents/${id}`,
  INSPECTION_SCHEDULED:      (id) => `/safety/inspections/${id}`,
  INSPECTION_STATUS_CHANGED: (id) => `/safety/inspections/${id}`,
};

const NotificationList = ({ notifications = [], onMarkRead }) => {
  const navigate = useNavigate();

  const handleClick = (notif) => {
    if (!notif.isRead && onMarkRead) onMarkRead(notif.notificationId);
    const linkFn = TYPE_LINK[notif.type];
    if (linkFn) navigate(linkFn(notif.relatedEntityId));
  };

  if (notifications.length === 0) {
    return <p className="text-muted text-center py-4">No notifications.</p>;
  }

  return (
    <ListGroup variant="flush">
      {notifications.map((notif) => (
        <ListGroup.Item
          key={notif.notificationId}
          action
          onClick={() => handleClick(notif)}
          className={notif.isRead ? '' : 'fw-semibold'}
          style={{
            backgroundColor: notif.isRead ? '' : '#f0f4ff',
            cursor: 'pointer',
            borderLeft: notif.isRead ? '' : '3px solid #0b64d6',
          }}
        >
          <div className="d-flex justify-content-between align-items-start gap-2">
            <div style={{ minWidth: 0 }}>
              <div className="mb-1">{notif.title}</div>
              <small className="text-muted">{notif.message}</small>
            </div>
            <div className="text-end flex-shrink-0">
              <small className="text-muted d-block">{dayjs(notif.createdAt).fromNow()}</small>
              {!notif.isRead && <Badge bg="primary" pill className="mt-1">New</Badge>}
            </div>
          </div>
        </ListGroup.Item>
      ))}
    </ListGroup>
  );
};

export default NotificationList;
