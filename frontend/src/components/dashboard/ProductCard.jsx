import React, { useState } from 'react';
import { Heart } from 'lucide-react';
import './ProductCard.css';

export default function ProductCard({ product, onCardClick }) {
  const { 
    id, 
    title, 
    categoryName, 
    price, 
    minParticipants, 
    currentParticipants, 
    status, 
    imageUrl,
    deadlineText
  } = product;

  const [isWished, setIsWished] = useState(false);

  const percentFilled = Math.round((currentParticipants / minParticipants) * 100);

  const handleWishToggle = (e) => {
    e.stopPropagation(); // 카드 클릭 이벤트와 겹치지 않게 방지
    setIsWished(!isWished);
  };

  return (
    <div className="glass-panel product-card" onClick={() => onCardClick(id)}>
      {/* Product Image and Badge */}
      <div className="image-wrapper">
        <img 
          src={imageUrl || "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=400&q=80"} 
          alt={title} 
          className="product-image"
          loading="lazy"
        />
        <div className={`status-badge ${status === 'SUCCESS' ? 'success' : 'recruiting'}`}>
          {status === 'SUCCESS' ? '모집 완료' : `모집 중 (D-${deadlineText})`}
        </div>
        <button 
          className={`wish-btn ${isWished ? 'wished' : ''}`} 
          onClick={handleWishToggle}
          aria-label="Wish list"
        >
          <Heart size={16} fill={isWished ? '#ef4444' : 'none'} />
        </button>
      </div>

      {/* Content Section */}
      <div className="product-content">
        <span className="category-tag">{categoryName}</span>
        <h3 className="product-title">{title}</h3>
        
        <div className="price-row">
          <span className="price-val">{price.toLocaleString()}</span>
          <span className="price-unit">원</span>
        </div>

        {/* Progress Gauge */}
        <div className="participants-container">
          <div className="participants-info">
            <span style={{ color: 'var(--text-muted)' }}>참여 현황 {currentParticipants}/{minParticipants}명</span>
            <span className="participants-percent">{percentFilled}%</span>
          </div>
          <div className="gauge-bar-bg">
            <div 
              className="gauge-bar-fill" 
              style={{ width: `${Math.min(100, percentFilled)}%` }}
            ></div>
          </div>
        </div>
      </div>
    </div>
  );
}
