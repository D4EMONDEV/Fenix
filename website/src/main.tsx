import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { App } from './App';
import './styles.css';

// The theme is written onto <html> before React renders, so the page never
// flashes the wrong colours on its way to the right ones.
const stored = localStorage.getItem('fenix-theme');
document.documentElement.dataset.theme =
  stored === 'light' || stored === 'dark'
    ? stored
    : window.matchMedia('(prefers-color-scheme: light)').matches
      ? 'light'
      : 'dark';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </StrictMode>,
);
