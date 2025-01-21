import React from 'react';
import Asset from '../components/Asset';

export default function Assets() {
  return (
    <div className="App">
      <div className="page-container" style={{ fontSize: '32px' }}>
        자산
        {data.assets.map((asset) => Asset(asset))}
      </div>
    </div>
  );
}

const data = {
  assets: [
    {
      name: 'Bitget',
      slug: 'bitget',
      image: '/images/bitget-circle.png',
      usd: 10000,
    },
    {
      name: 'Binance',
      slug: 'binance',
      image: '/images/binance-circle.png',
      usd: 10000,
    },
    {
      name: 'Upbit',
      slug: 'upbit',
      image: '/images/upbit-circle.png',
      usd: 10000,
    },
  ],
};
