// src/layouts/RootLayout.jsx
import { Outlet } from 'react-router-dom';
import Navbar from '@/components/ui/Navbar';

export default function RootLayout() {
  return (
    <div>
      <Navbar />
      <div className="pt-16 h-[calc(100vh-4rem)]">
        <Outlet />
      </div>
    </div>
  );
}