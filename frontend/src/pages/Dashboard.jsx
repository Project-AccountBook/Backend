import React, { useState } from 'react';
import { 
  ArrowUpRight, 
  ChevronLeft, 
  ChevronRight, 
  Calendar,
  DollarSign, 
  TrendingDown, 
  TrendingUp, 
  Wallet,
  ShoppingBag,
  ArrowRight
} from 'lucide-react';
import './Dashboard.css';

export default function Dashboard() {
  const [activeTab, setActiveTab] = useState('dashboard');
  const [activeDonutTab, setActiveDonutTab] = useState('expense');

  // 1열: 지출 대분류 도넛 차트 데이터
  const donutData = [
    { name: '식비', percent: 36, amount: 2500000, color: '#3b82f6' },
    { name: '주거비', percent: 22, amount: 1500000, color: '#10b981' },
    { name: '육아용품', percent: 17, amount: 1200000, color: '#ef4444' },
    { name: '교통/통신', percent: 13, amount: 859106, color: '#6366f1' },
    { name: '기타/예비비', percent: 12, amount: 800000, color: '#94a3b8' },
  ];

  // 1열 하단: 하위 카테고리 상세 도넛 차트 데이터
  const subDonutData = [
    { name: '식비 > 외식', percent: 17, amount: 1200000, color: '#60a5fa' },
    { name: '식비 > 식자재 (공구)', percent: 19, amount: 1300000, color: '#3b82f6' },
    { name: '주거비 > 대출이자', percent: 15, amount: 1000000, color: '#10b981' },
    { name: '주거비 > 관리비', percent: 7, amount: 500000, color: '#f87171' },
    { name: '육아용품 > 기저귀/분유', percent: 17, amount: 1200000, color: '#a7f3d0' },
  ];

  // 2열 하단: 월별 현금흐름 데이터 테이블 (6개월)
  const monthlyFlows = [
    { month: '1월', income: '2,820', expense: '663', save: '300', ratio: '10.6%' },
    { month: '2월', income: '1,390', expense: '704', save: '300', ratio: '21.6%' },
    { month: '3월', income: '1,388', expense: '691', save: '300', ratio: '21.6%' },
    { month: '4월', income: '1,392', expense: '651', save: '300', ratio: '21.6%' },
    { month: '5월', income: '1,393', expense: '682', save: '300', ratio: '21.5%' },
    { month: '6월', income: '1,455', expense: '680', save: '300', ratio: '20.6%' },
  ];

  // 3열 하단: 자산 구성 비율
  const assetRatios = [
    { name: '유동자산', amount: '28.5M', percent: 4.3, colorClass: 'blue' },
    { name: '투자자산', amount: '64.5M', percent: 9.7, colorClass: 'teal' },
    { name: '비유동자산', amount: '570.0M', percent: 86.0, colorClass: 'purple' },
  ];

  // 하단: 동네 공동구매 목록 데이터
  const mockGroupPurchases = [
    {
      id: 1,
      category: '생필품',
      title: '친환경 세탁세제 대용량 공구',
      status: 'RECRUITING',
      statusText: '모집중 (D-2)',
      rate: 125,
      price: '15,000',
      colorClass: 'green'
    },
    {
      id: 2,
      category: '식품',
      title: '제주 유기농 흑돼지 1kg',
      status: 'WARNING',
      statusText: '마감임박 (D-1)',
      rate: 85,
      price: '28,000',
      colorClass: 'blue'
    },
    {
      id: 3,
      category: '육아용품',
      title: '프리미엄 기저귀 박스떼기',
      status: 'CLOSED',
      statusText: '진행완료 (마감)',
      rate: 100,
      price: '45,000',
      colorClass: 'green'
    }
  ];

  return (
    <div className="dashboard-wrapper">
      {/* 1. 상단 탭 및 날짜 컨트롤러 */}
      <div className="tab-date-wrapper">
        <div className="dashboard-tabs">
          <button 
            className={`dashboard-tab ${activeTab === 'dashboard' ? 'active' : ''}`}
            onClick={() => setActiveTab('dashboard')}
          >
            대시보드
          </button>
          <button 
            className={`dashboard-tab ${activeTab === 'analysis' ? 'active' : ''}`}
            onClick={() => setActiveTab('analysis')}
          >
            소비 분석
          </button>
          <button 
            className={`dashboard-tab ${activeTab === 'budget' ? 'active' : ''}`}
            onClick={() => setActiveTab('budget')}
          >
            예산 관리
          </button>
          <button 
            className={`dashboard-tab ${activeTab === 'portfolio' ? 'active' : ''}`}
            onClick={() => setActiveTab('portfolio')}
          >
            자산 포트폴리오
          </button>
          <button 
            className={`dashboard-tab ${activeTab === 'compare' ? 'active' : ''}`}
            onClick={() => setActiveTab('compare')}
          >
            이웃 자산 비교
          </button>
        </div>

        <div className="date-controller">
          <button className="nav-arrow-btn" aria-label="Previous month">
            <ChevronLeft size={16} />
          </button>
          <button className="date-display-btn">
            <Calendar size={14} />
            <span>2026년 6월 소비분석표</span>
          </button>
          <button className="nav-arrow-btn" aria-label="Next month">
            <ChevronRight size={16} />
          </button>
        </div>
      </div>

      {/* 2. 4종 메트릭 카드 */}
      <div className="metric-grid">
        {/* 카드 1: 월 총 수입 */}
        <div className="metric-card">
          <div className="metric-card-header">
            <span className="metric-card-title">월 총 수입</span>
            <TrendingUp size={16} className="metric-card-icon" style={{ color: 'var(--color-green)' }} />
          </div>
          <span className="metric-card-value">+14,546,049</span>
          <div className="metric-card-footer">
            <span className="badge badge-success">전월 대비 +620,883원</span>
          </div>
        </div>

        {/* 카드 2: 월 총 지출 */}
        <div className="metric-card">
          <div className="metric-card-header">
            <span className="metric-card-title">월 총 지출</span>
            <TrendingDown size={16} className="metric-card-icon" style={{ color: 'var(--color-red)' }} />
          </div>
          <span className="metric-card-value">-6,859,106</span>
          <div className="metric-card-footer">
            <span className="badge badge-danger">전월 대비 +44,056원</span>
          </div>
        </div>

        {/* 카드 3: 가계부 예산 잔액 */}
        <div className="metric-card">
          <div className="metric-card-header">
            <span className="metric-card-title">가계부 예산 잔액</span>
            <Wallet size={16} className="metric-card-icon" />
          </div>
          <span className="metric-card-value">3,140,894</span>
          <div className="metric-card-footer">
            <span className="metric-card-footer-text">월 예산: 10,000,000원</span>
          </div>
        </div>

        {/* 카드 4: 현재 종합 자산 (검은색 배경) */}
        <div className="metric-card black-theme">
          <div className="metric-card-header">
            <span className="metric-card-title">현재 종합 자산</span>
            <ArrowUpRight size={16} className="metric-card-icon" />
          </div>
          <span className="metric-card-value">663,000,000</span>
          <div className="metric-card-footer">
            <span className="metric-card-footer-text">유동: 28.5M │ 투자: 64.5M</span>
          </div>
        </div>
      </div>

      {/* 3. 3열 분석 차트 그리드 */}
      <div className="charts-grid">
        
        {/* 1열: 지출 카테고리 분석 카드 */}
        <div className="dashboard-card chart-card">
          <div className="chart-card-title-row">
            <h3 className="chart-card-title">소비 분석</h3>
            <div className="donut-tabs-row">
              <button className="donut-tab">수입</button>
              <button className={`donut-tab ${activeDonutTab === 'expense' ? 'active' : ''}`} onClick={() => setActiveDonutTab('expense')}>지출</button>
              <button className="donut-tab">저축</button>
            </div>
          </div>

          {/* 도넛 그래프 영역 (SVG 활용) */}
          <div className="donut-chart-container">
            <svg width="150" height="150" viewBox="0 0 42 42">
              <circle cx="21" cy="21" r="15.915" fill="transparent" stroke="#e2e8f0" strokeWidth="4.2"></circle>
              {/* 식비 36%: 대략 대시값 36, offset 100 */}
              <circle cx="21" cy="21" r="15.915" fill="transparent" stroke="#3b82f6" strokeWidth="4.2" strokeDasharray="36 64" strokeDashoffset="100"></circle>
              {/* 주거비 22%: 대시값 22, offset 64 */}
              <circle cx="21" cy="21" r="15.915" fill="transparent" stroke="#10b981" strokeWidth="4.2" strokeDasharray="22 78" strokeDashoffset="64"></circle>
              {/* 육아용품 17%: 대시값 17, offset 42 */}
              <circle cx="21" cy="21" r="15.915" fill="transparent" stroke="#ef4444" strokeWidth="4.2" strokeDasharray="17 83" strokeDashoffset="42"></circle>
              {/* 교통/통신 13%: 대시값 13, offset 25 */}
              <circle cx="21" cy="21" r="15.915" fill="transparent" stroke="#6366f1" strokeWidth="4.2" strokeDasharray="13 87" strokeDashoffset="25"></circle>
              {/* 기타/예비비 12%: 대시값 12, offset 12 */}
              <circle cx="21" cy="21" r="15.915" fill="transparent" stroke="#94a3b8" strokeWidth="4.2" strokeDasharray="12 88" strokeDashoffset="12"></circle>
            </svg>
            <div className="donut-label-center">
              <span className="donut-label-value">6,859,106</span>
              <span className="donut-label-title">총 지출 (원)</span>
            </div>
          </div>

          {/* 대분류 범례 */}
          <div className="donut-legend-list">
            {donutData.map(item => (
              <div key={item.name} className="donut-legend-item">
                <div className="legend-left">
                  <span className="legend-color-dot" style={{ backgroundColor: item.color }}></span>
                  <span className="legend-name">{item.name}</span>
                </div>
                <div className="legend-right">
                  <span className="legend-percent">{item.percent}%</span>
                  <span className="legend-amount">{item.amount.toLocaleString()}</span>
                </div>
              </div>
            ))}
          </div>

          {/* 하위 카테고리 상세 도넛 차트 */}
          <div className="chart-card-title-row" style={{ marginTop: '16px', borderBottom: '1px solid var(--border-color)' }}>
            <h3 className="chart-card-title">하위 카테고리 상세</h3>
          </div>

          <div className="donut-chart-container">
            <svg width="150" height="150" viewBox="0 0 42 42">
              <circle cx="21" cy="21" r="15.915" fill="transparent" stroke="#e2e8f0" strokeWidth="4.2"></circle>
              {/* 식비>외식 17% */}
              <circle cx="21" cy="21" r="15.915" fill="transparent" stroke="#60a5fa" strokeWidth="4.2" strokeDasharray="17 83" strokeDashoffset="100"></circle>
              {/* 식비>식자재 19% */}
              <circle cx="21" cy="21" r="15.915" fill="transparent" stroke="#3b82f6" strokeWidth="4.2" strokeDasharray="19 81" strokeDashoffset="83"></circle>
              {/* 주거비>대출이자 15% */}
              <circle cx="21" cy="21" r="15.915" fill="transparent" stroke="#10b981" strokeWidth="4.2" strokeDasharray="15 85" strokeDashoffset="64"></circle>
              {/* 주거비>관리비 7% */}
              <circle cx="21" cy="21" r="15.915" fill="transparent" stroke="#f87171" strokeWidth="4.2" strokeDasharray="7 93" strokeDashoffset="49"></circle>
              {/* 육아용품>기저귀/분유 17% */}
              <circle cx="21" cy="21" r="15.915" fill="transparent" stroke="#a7f3d0" strokeWidth="4.2" strokeDasharray="17 83" strokeDashoffset="42"></circle>
            </svg>
            <div className="donut-label-center">
              <span className="donut-label-value">5,200,000</span>
              <span className="donut-label-title">상위 5종 (원)</span>
            </div>
          </div>

          {/* 하위 범례 */}
          <div className="donut-legend-list">
            {subDonutData.map(item => (
              <div key={item.name} className="donut-legend-item">
                <div className="legend-left">
                  <span className="legend-color-dot" style={{ backgroundColor: item.color }}></span>
                  <span className="legend-name">{item.name}</span>
                </div>
                <div className="legend-right">
                  <span className="legend-percent">{item.percent}%</span>
                  <span className="legend-amount">{item.amount.toLocaleString()}</span>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* 2열: 월별 현금흐름 분석 카드 */}
        <div className="dashboard-card chart-card">
          <div className="chart-card-title-row">
            <h3 className="chart-card-title">월별 현금흐름 분석</h3>
          </div>

          {/* 평균 요약 수치 */}
          <div className="bar-chart-stats">
            <div className="stat-item">
              <div className="stat-label-group">
                <span className="stat-dot blue"></span>
                <span>평균 수입금액</span>
              </div>
              <span className="stat-value">16,400,000원</span>
              <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>최근 6개월 평균</span>
            </div>
            <div className="stat-item">
              <div className="stat-label-group">
                <span className="stat-dot red"></span>
                <span>평균 지출금액</span>
              </div>
              <span className="stat-value">6,750,000원</span>
              <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>최근 6개월 평균</span>
            </div>
          </div>

          {/* 막대 차트 (CSS Flex 구현) */}
          <div className="bar-chart-visual">
            {[
              { m: '1월', h1: 85, h2: 30, h3: 20 },
              { m: '2월', h1: 65, h2: 25, h3: 35 },
              { m: '3월', h1: 60, h2: 35, h3: 32 },
              { m: '4월', h1: 65, h2: 45, h3: 30 },
              { m: '5월', h1: 70, h2: 40, h3: 38 },
              { m: '6월', h1: 80, h2: 50, h3: 42 }
            ].map(col => (
              <div key={col.m} className="bar-column-wrapper">
                <div className="bar-stack">
                  <div className="bar-fill income" style={{ height: `${col.h1}%` }}></div>
                  <div className="bar-fill save" style={{ height: `${col.h2}%` }}></div>
                  <div className="bar-fill expense" style={{ height: `${col.h3}%` }}></div>
                </div>
                <span className="bar-month-label">{col.m}</span>
              </div>
            ))}
          </div>

          {/* 현금흐름 상세 테이블 */}
          <div style={{ marginTop: '10px' }}>
            <table className="data-table">
              <thead>
                <tr>
                  <th>월</th>
                  <th>수입</th>
                  <th>지출</th>
                  <th>저축</th>
                  <th>저축률</th>
                </tr>
              </thead>
              <tbody>
                {monthlyFlows.map(flow => (
                  <tr key={flow.month}>
                    <td className="table-month">{flow.month}</td>
                    <td className="table-income">{flow.income}</td>
                    <td className="table-expense">{flow.expense}</td>
                    <td className="table-save">{flow.save}</td>
                    <td className="table-ratio">{flow.ratio}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        {/* 3열: 월흐름 추세 & 자산 구성 비율 카드 */}
        <div className="dashboard-card chart-card">
          <div className="chart-card-title-row">
            <h3 className="chart-card-title">월흐름 추세</h3>
            <span className="chart-card-subtitle">2026년 반기</span>
          </div>

          {/* 꺾은선 범례 */}
          <div className="line-chart-legend">
            <div className="legend-indicator">
              <span className="legend-color-dot" style={{ backgroundColor: 'var(--color-primary)' }}></span>
              <span>수입</span>
            </div>
            <div className="legend-indicator">
              <span className="legend-color-dot" style={{ backgroundColor: 'var(--color-green)' }}></span>
              <span>저축</span>
            </div>
            <div className="legend-indicator">
              <span className="legend-color-dot" style={{ backgroundColor: 'var(--color-red)' }}></span>
              <span>지출</span>
            </div>
          </div>

          {/* 꺾은선 차트 영역 (SVG) */}
          <div className="line-chart-svg-container">
            <svg width="100%" height="100%" viewBox="0 0 300 120" preserveAspectRatio="none">
              {/* 가이드라인 grid */}
              <line x1="0" y1="20" x2="300" y2="20" stroke="#f1f5f9" strokeWidth="1" />
              <line x1="0" y1="60" x2="300" y2="60" stroke="#f1f5f9" strokeWidth="1" />
              <line x1="0" y1="100" x2="300" y2="100" stroke="#f1f5f9" strokeWidth="1" />
              
              {/* 수입 라인 (Blue) */}
              <path d="M 10 30 L 60 45 L 110 50 L 160 35 L 210 40 L 260 25" fill="none" stroke="var(--color-primary)" strokeWidth="2" />
              {/* 저축 라인 (Green) */}
              <path d="M 10 80 L 60 70 L 110 75 L 160 85 L 210 78 L 260 68" fill="none" stroke="var(--color-green)" strokeWidth="2" strokeDasharray="3 3" />
              {/* 지출 라인 (Red) */}
              <path d="M 10 95 L 60 90 L 110 85 L 160 92 L 210 90 L 260 88" fill="none" stroke="var(--color-red)" strokeWidth="2" />
              
              {/* 포인트 점찍기 */}
              <circle cx="260" cy="25" r="3.5" fill="var(--color-primary)" />
              <circle cx="260" cy="68" r="3.5" fill="var(--color-green)" />
              <circle cx="260" cy="88" r="3.5" fill="var(--color-red)" />
            </svg>
          </div>

          {/* 자산 요약 박스 */}
          <div className="asset-save-summary-box">
            <div className="summary-box-item">
              <span className="summary-box-label">총 자산</span>
              <span className="summary-box-val">663,000,000원</span>
            </div>
            <div className="summary-box-item">
              <span className="summary-box-label">이달 저축</span>
              <span className="summary-box-val" style={{ color: 'var(--color-green)' }}>3,000,000원</span>
            </div>
          </div>

          {/* 자산 구성 비율 바 그래프 */}
          <div className="chart-card-title-row" style={{ marginTop: '8px', borderBottom: '1px solid var(--border-color)' }}>
            <h3 className="chart-card-title">상세 내역</h3>
          </div>

          {/* 전월/연초 대비 요약 텍스트 */}
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8rem', padding: '0 4px' }}>
            <span style={{ color: 'var(--text-muted)' }}>전월 대비</span>
            <span className="badge badge-success" style={{ padding: '2px 8px' }}>+ 4.5%</span>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8rem', padding: '0 4px', marginTop: '-8px' }}>
            <span style={{ color: 'var(--text-muted)' }}>연초 대비</span>
            <span className="badge badge-success" style={{ padding: '2px 8px' }}>+ 8.2%</span>
          </div>

          {/* 세부 자산 구성 비율 게이지 바 */}
          <div className="asset-ratio-list" style={{ marginTop: '4px' }}>
            {assetRatios.map(ratio => (
              <div key={ratio.name} className="asset-ratio-item">
                <div className="ratio-label-row">
                  <span className="ratio-name">{ratio.name}</span>
                  <span className="ratio-val">{ratio.amount} ({ratio.percent}%)</span>
                </div>
                <div className="ratio-progress-bg">
                  <div 
                    className={`ratio-progress-fill ${ratio.colorClass}`}
                    style={{ width: `${ratio.percent}%` }}
                  ></div>
                </div>
              </div>
            ))}
          </div>
        </div>

      </div>

      {/* 4. 하단 동네 공동구매 현황 */}
      <div className="gp-section">
        <div className="gp-section-header">
          <div className="gp-section-title-group">
            <div className="gp-icon-wrapper">
              <ShoppingBag size={18} />
            </div>
            <h2 className="gp-section-title">동네 공동구매 현황</h2>
          </div>
          <button className="gp-more-btn">
            <span>전체보기</span>
            <ArrowRight size={14} />
          </button>
        </div>

        {/* 3개 공동구매 카드 가로 그리드 */}
        <div className="gp-grid">
          {mockGroupPurchases.map(gp => (
            <div key={gp.id} className="gp-card">
              <div className="gp-badge-row">
                <span className="gp-cat-badge">{gp.category}</span>
                <span className={`gp-status-badge ${gp.status === 'RECRUITING' ? 'd-day' : gp.status === 'WARNING' ? 'warning' : 'closed'}`}>
                  {gp.statusText}
                </span>
              </div>
              
              <h4 className="gp-card-title">{gp.title}</h4>

              <div className="gp-progress-container">
                <div className="gp-progress-label">
                  <span style={{ color: 'var(--text-muted)', fontSize: '0.75rem' }}>목표 달성률</span>
                  <span style={{ color: gp.colorClass === 'green' ? 'var(--color-green)' : 'var(--color-primary)', fontSize: '0.75rem' }}>
                    {gp.rate}%
                  </span>
                </div>
                <div className="gp-progress-bar-bg">
                  <div 
                    className={`gp-progress-bar-fill ${gp.colorClass}`}
                    style={{ width: `${Math.min(100, gp.rate)}%` }}
                  ></div>
                </div>
              </div>

              <div className="gp-price-row">
                <span className="gp-price-label">공구 가격</span>
                <span className="gp-price-val">{gp.price} 원</span>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
