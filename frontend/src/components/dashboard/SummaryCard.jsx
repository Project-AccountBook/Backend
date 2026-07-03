import React from 'react';
import { TrendingUp, Plus, ArrowUpRight, CheckCircle2 } from 'lucide-react';
import './SummaryCard.css';

export default function SummaryCard({ type, data, onActionClick }) {
  if (type === 'expense') {
    const { totalExpense, averageDaily, changeRate } = data;
    return (
      <div className="glass-panel summary-card">
        <div className="summary-header">
          <span className="summary-title">이번 달 지출 현황</span>
          <div className="summary-icon-container blue">
            <TrendingUp size={18} />
          </div>
        </div>

        <div className="summary-value-wrapper">
          <span className="summary-value">{totalExpense.toLocaleString()}원</span>
          <span className="summary-subtext">하루 평균 약 {averageDaily.toLocaleString()}원 지출</span>
        </div>

        <div className="progress-container">
          <div className="progress-label-row">
            <span style={{ color: 'var(--text-muted)' }}>지난달 대비 지출 추이</span>
            <span style={{ color: changeRate < 0 ? 'var(--color-mint)' : 'var(--color-danger)', fontWeight: '600' }}>
              {changeRate < 0 ? `-${Math.abs(changeRate)}% 감소` : `+${changeRate}% 증가`}
            </span>
          </div>
          <div className="progress-bar-bg">
            <div 
              className="progress-bar-fill blue" 
              style={{ width: `${Math.min(100, Math.max(10, 100 - changeRate))}%` }}
            ></div>
          </div>
        </div>

        <div className="quick-actions">
          <button className="action-btn primary" onClick={() => onActionClick('add-expense')}>
            <Plus size={16} />
            <span>내역 추가</span>
          </button>
          <button className="action-btn secondary" onClick={() => onActionClick('view-history')}>
            <span>상세 내역</span>
            <ArrowUpRight size={14} />
          </button>
        </div>
      </div>
    );
  }

  if (type === 'budget') {
    const { totalBudget, remainingBudget, percentUsed } = data;
    return (
      <div className="glass-panel summary-card">
        <div className="summary-header">
          <span className="summary-title">예산 및 잔액 현황</span>
          <div className="summary-icon-container mint">
            <CheckCircle2 size={18} />
          </div>
        </div>

        <div className="summary-value-wrapper">
          <span className="summary-value">{remainingBudget.toLocaleString()}원 남음</span>
          <span className="summary-subtext">총 예산 {totalBudget.toLocaleString()}원 설정</span>
        </div>

        <div className="progress-container">
          <div className="progress-label-row">
            <span style={{ color: 'var(--text-muted)' }}>예산 소진율</span>
            <span style={{ color: percentUsed > 90 ? 'var(--color-danger)' : 'var(--text-main)' }}>
              {percentUsed}% 사용
            </span>
          </div>
          <div className="progress-bar-bg">
            <div 
              className="progress-bar-fill mint" 
              style={{ width: `${Math.min(100, percentUsed)}%` }}
            ></div>
          </div>
        </div>

        <div className="quick-actions">
          <button className="action-btn primary" onClick={() => onActionClick('create-group-purchase')}>
            <Plus size={16} />
            <span>공구 열기</span>
          </button>
          <button className="action-btn secondary" onClick={() => onActionClick('edit-budget')}>
            <span>예산 설정</span>
            <ArrowUpRight size={14} />
          </button>
        </div>
      </div>
    );
  }

  return null;
}
