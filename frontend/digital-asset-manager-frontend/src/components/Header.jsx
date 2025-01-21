import React from 'react';
import { Link } from 'react-router-dom';

export default function Header() {
  return (
    <div className="header-container">
      <div className="header-wrap">
        <div className="header-left-wrap">
          <Link to="/">
            <img id="logo" src="/images/logo.webp" alt="로고" />
          </Link>
          <ul>
            <li>
              <Link className="header-nav-item" to={'/assets'}>
                자산
              </Link>
            </li>
            <li>
              <Link className="header-nav-item" to={'/mypage'}>
                내 정보
              </Link>
            </li>
          </ul>
        </div>
      </div>
    </div>
  );
}
