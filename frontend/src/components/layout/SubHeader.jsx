import React from 'react';
import { Bell, User, Sliders } from 'lucide-react';
import './SubHeader.css';

export default function SubHeader() {
  const handleBudgetSettings = () => {
    alert('가계부 예산 설정 메뉴로 이동합니다.');
  };

  return (
    <div className="subheader">
      {/* Left Connection Status */}
      <div className="status-badge-container">
        <span className="status-indicator"></span>
        <span className="status-badge-text">가계부 접속중</span>
      </div>

      {/* Right Header Actions */}
      <div className="subheader-actions">
        <button className="bell-btn" aria-label="Notifications">
          <Bell size={18} />
        </button>

        <button className="user-avatar-btn" aria-label="Profile">
          <div className="user-avatar-circle">
            <User size={16} />
          </div>
        </button>

        <button className="budget-settings-btn" onClick={handleBudgetSettings}>
          <Sliders size={14} />
          <span>예산 설정</span>
        </button>
      </div>
    </div>
  );
}
