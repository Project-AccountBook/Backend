import React, { useState } from 'react';
import { 
  LayoutDashboard, 
  FileText, 
  PieChart, 
  BarChart3, 
  MessageSquare, 
  Lightbulb, 
  ShoppingBag, 
  Settings,
  Users
} from 'lucide-react';
import './Sidebar.css';

export default function Sidebar() {
  const [activeMenu, setActiveMenu] = useState('dashboard');

  const menuItems = [
    { id: 'dashboard', name: '대시보드', icon: LayoutDashboard },
    { id: 'history', name: '내역 관리', icon: FileText },
    { id: 'analysis', name: '소비/예산 분석', icon: PieChart },
    { id: 'compare', name: '이웃 자산 비교', icon: BarChart3 },
    { id: 'qna', name: 'Q&A 게시판', icon: MessageSquare },
    { id: 'knowhow', name: '노하우 공유', icon: Lightbulb },
    { id: 'grouppurchase', name: '동네 공동구매', icon: ShoppingBag, hasHot: true },
  ];

  return (
    <aside className="sidebar">
      {/* Logo Container */}
      <div className="logo-container">
        <div className="logo-icon-wrapper">
          <Users size={18} fill="#ffffff" />
        </div>
        <span className="logo-text">Joint Living</span>
      </div>

      {/* Menu List */}
      <nav className="menu-list">
        {menuItems.map(item => {
          const IconComponent = item.icon;
          return (
            <button
              key={item.id}
              className={`menu-item ${activeMenu === item.id ? 'active' : ''}`}
              onClick={() => setActiveMenu(item.id)}
            >
              <div className="menu-item-left">
                <IconComponent size={18} />
                <span>{item.name}</span>
              </div>
              {item.hasHot && <span className="hot-badge">HOT</span>}
            </button>
          );
        })}
      </nav>

      {/* Footer / Settings */}
      <div className="sidebar-footer">
        <button 
          className={`menu-item ${activeMenu === 'settings' ? 'active' : ''}`}
          onClick={() => setActiveMenu('settings')}
        >
          <div className="menu-item-left">
            <Settings size={18} />
            <span>설정 및 프로필</span>
          </div>
        </button>
      </div>
    </aside>
  );
}
