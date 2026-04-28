import React, { useState, useEffect, useCallback } from 'react';
import { Card, Button, Alert } from 'react-bootstrap';
import DashboardLayout from '../../components/layout/DashboardLayout';
import { useAuth } from '../../context/AuthContext';
import API from '../../api/axiosInstance';
import NotificationList from '../../components/safety/NotificationList';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import { toast } from 'react-toastify';

const NotificationsPage = () => {
  const { user, logout } = useAuth();
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [markingAll, setMarkingAll] = useState(false);

  const fetchNotifications = useCallback(async () => {
    if (!user?.userId) return;
    setLoading(true);
    try {
      const res = await API.get(`/api/safety/notifications/user/${user.userId}`);
      setNotifications(res.data || []);
    } catch (err) {
      toast.error('Failed to load notifications');
    } finally {
      setLoading(false);
    }
  }, [user?.userId]);

  useEffect(() => { fetchNotifications(); }, [fetchNotifications]);

  const handleMarkRead = async (notificationId) => {
    try {
      const res = await API.patch(`/api/safety/notifications/${notificationId}/read`);
      setNotifications((prev) =>
        prev.map((n) => (n.notificationId === notificationId ? res.data : n))
      );
    } catch {
      // silently fail — navigation still works
    }
  };

  const handleMarkAllRead = async () => {
    setMarkingAll(true);
    try {
      await API.patch(`/api/safety/notifications/mark-all-read/${user.userId}`);
      setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })));
      toast.success('All notifications marked as read');
    } catch (err) {
      toast.error('Failed to mark all as read');
    } finally {
      setMarkingAll(false);
    }
  };

  const unreadCount = notifications.filter((n) => !n.isRead).length;

  return (
    <DashboardLayout title="Notifications" onLogout={logout}>
      <nav aria-label="breadcrumb">
        <ol className="breadcrumb bg-transparent p-0 mb-2">
          <li className="breadcrumb-item active">Notifications</li>
        </ol>
      </nav>

      <div className="d-flex justify-content-between align-items-center mb-3">
        <h5 className="heading-primary mb-0">
          Notifications{unreadCount > 0 && <span className="ms-2 badge bg-primary">{unreadCount} unread</span>}
        </h5>
        {unreadCount > 0 && (
          <Button size="sm" variant="outline-primary" onClick={handleMarkAllRead} disabled={markingAll}>
            {markingAll ? 'Marking…' : 'Mark All as Read'}
          </Button>
        )}
      </div>

      <Card className="shadow-sm">
        <Card.Body className="p-0">
          {loading ? (
            <LoadingSpinner message="Loading notifications..." />
          ) : notifications.length === 0 ? (
            <Alert variant="info" className="m-3">You have no notifications.</Alert>
          ) : (
            <NotificationList notifications={notifications} onMarkRead={handleMarkRead} />
          )}
        </Card.Body>
      </Card>
    </DashboardLayout>
  );
};

export default NotificationsPage;
