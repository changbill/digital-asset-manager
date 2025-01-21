import React from 'react';
import { useNavigate } from 'react-router-dom';

export default function Asset(asset) {
  const navigate = useNavigate();
  const onClickAssetItem = () => {
    navigate(`/assets/${asset.name}`, {
      state: asset,
    });
  };
  return (
    <div
      className="asset-container"
      key={asset.slug}
      onClick={onClickAssetItem}
    >
      <img src={asset.image} alt="자산 내용" />
      <div className="asset-info">
        <h4 className="name">{asset.name}</h4>
        <span className="usd">${asset.usd.toLocaleString()}</span>
      </div>
    </div>
  );
}
