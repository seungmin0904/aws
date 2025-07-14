// src/layouts/RootLayout.jsx
import { Outlet } from 'react-router-dom';
import Navbar from '@/components/ui/Navbar';

export default function RootLayout({user, onLogout }) {
  return (
    <div>
      <Navbar user={user} onLogout={onLogout} />
      <div className="pt-16 h-[calc(100vh-4rem)]">
        <Outlet />
      </div>
    </div>
  );
}