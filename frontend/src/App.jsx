import React from 'react';
import Sidebar from './components/layout/Sidebar';
import SubHeader from './components/layout/SubHeader';
import Dashboard from './pages/Dashboard';

function App() {
  return (
    <div className="app-layout">
      <Sidebar />
      <div className="main-content">
        <SubHeader />
        <main>
          <Dashboard />
        </main>
      </div>
    </div>
  );
}

export default App;
