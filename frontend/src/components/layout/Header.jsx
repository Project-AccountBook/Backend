import React, { useState } from 'react';
import { Bell, Wallet, Users, LayoutDashboard } from 'lucide-react';
import './Header.css';

export default function Header() {
  const [activeTab, setActiveTab] = useState('dashboard');

  return (
    <header className="header">
      <div className="container header-container">
        {/* Logo Section */}
        <div className="logo-section">
          <Wallet className="logo-icon" size={24} />
          <span>Momny</span>
        </div>

        {/* Navigation Links */}
        <nav className="nav-links">
          <button 
            className={`nav-item ${activeTab === 'dashboard' ? 'active' : ''}`}
            onClick={() => setActiveTab('dashboard')}
          >
            <LayoutDashboard className="nav-icon" size={18} />
            <span>대시보드</span>
          </button>
          <button 
            className={`nav-item ${activeTab === 'account' ? 'active' : ''}`}
            onClick={() => setActiveTab('account')}
          >
            <Wallet className="nav-icon" size={18} />
            <span>가계부</span>
          </button>
          <button 
            className={`nav-item ${activeTab === 'grouppurchase' ? 'active' : ''}`}
            onClick={() => setActiveTab('grouppurchase')}
          >
            <Users className="nav-icon" size={18} />
            <span>공동구매</span>
          </button>
        </nav>

        {/* Right Actions Section */}
        <div className="right-section">
          <button className="notification-btn" aria-label="Notifications">
            <Bell size={20} />
            <span className="notification-dot"></span>
          </button>
          
          <div className="profile-card">
            <div className="profile-avatar">OS</div>
            <span className="profile-name">온샘님</span>
          </div>
        </div>
      </div>
    </header>
  );
}
