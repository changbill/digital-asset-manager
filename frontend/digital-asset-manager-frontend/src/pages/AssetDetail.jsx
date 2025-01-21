import React from 'react';
import { useLocation, useParams } from 'react-router-dom';

export default function AssetDetail() {
  const { name } = useParams();
  const { state } = useLocation();
  console.log(name);
  return (
    <div className="page-container">
      <div style={{ display: 'flex' }}>
        <img
          style={{ width: '100px' }}
          src={`/images/` + name + `-circle.png`}
          alt="자산 이미지"
        />
        <div>
          <div style={{ fontSize: '32px' }}>{name}</div>
        </div>
      </div>
    </div>
  );
}
