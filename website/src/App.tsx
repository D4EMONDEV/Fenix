import { Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { useEffect } from 'react';
import { Header } from './components/Header';
import { Footer } from './components/Footer';
import { Home } from './pages/Home';
import { Docs } from './pages/Docs';
import { latestVersion } from './lib/content';

export function App() {
  const { pathname, hash } = useLocation();

  // A new page starts at the top; a link to a heading starts at the heading.
  // Without this a router keeps the previous scroll position, and a long page
  // opens halfway down for no reason the reader can see.
  useEffect(() => {
    if (hash) {
      document.getElementById(hash.slice(1))?.scrollIntoView();
    } else {
      window.scrollTo(0, 0);
    }
  }, [pathname, hash]);

  return (
    <>
      <Header />
      <main id="content">
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/docs" element={<Navigate to={`/docs/${latestVersion}/index`} replace />} />
          <Route path="/docs/:version/*" element={<Docs />} />
          <Route path="/why" element={<Navigate to={`/docs/${latestVersion}/why/comparison`} replace />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
      <Footer />
    </>
  );
}
