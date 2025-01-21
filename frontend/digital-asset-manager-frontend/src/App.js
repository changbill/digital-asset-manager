import { BrowserRouter, Route, Routes } from 'react-router-dom';
import Home from './pages/Home';
import Assets from './pages/Assets';
import MyPage from './pages/MyPage';
import NotFound from './pages/NotFound';
import Header from './components/Header';
import AssetDetail from './pages/AssetDetail';

function App() {
  return (
    <div className="root-wrap">
      <BrowserRouter>
        <Header />
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/assets/:name" element={<AssetDetail />} />
          <Route path="/assets" element={<Assets />} />
          <Route path="/mypage" element={<MyPage />} />
          <Route path="/*" element={<NotFound />} />
        </Routes>
      </BrowserRouter>
    </div>
  );
}

export default App;
