import React from 'react';
import { useApp } from '../context/AppContext';
import { RadioTower, Map, History, Settings } from 'lucide-react';

export const Navigation: React.FC = () => {
  const { activeTab, setActiveTab, pendingCount, reports, currentRole } = useApp();

  const isAdmin = currentRole === 'ADMIN';

  return (
    <nav className="fixed bottom-0 left-0 right-0 z-30 bg-white border-t border-slate-200 shadow-lg sm:static sm:border-t-0 sm:shadow-none sm:bg-slate-50 sm:border-b sm:border-slate-200">
      <div className="max-w-5xl mx-auto px-2 flex justify-around sm:justify-start sm:space-x-4">
        {/* Site Audit Tab */}
        <button
          onClick={() => setActiveTab('AUDIT')}
          className={`flex-1 sm:flex-none flex flex-col sm:flex-row items-center justify-center py-2.5 sm:py-3 px-3 text-xs sm:text-sm font-semibold transition-all relative ${
            activeTab === 'AUDIT'
              ? 'text-telecom-navy font-bold after:absolute after:bottom-0 after:left-2 after:right-2 sm:after:left-0 sm:after:right-0 after:h-0.5 after:bg-blue-600'
              : 'text-slate-500 hover:text-slate-800'
          }`}
        >
          <RadioTower className={`w-5 h-5 sm:mr-2 ${activeTab === 'AUDIT' ? 'text-blue-600' : 'text-slate-400'}`} />
          <span>Audit</span>
        </button>

        {/* Sites Map Tab */}
        <button
          onClick={() => setActiveTab('MAP')}
          className={`flex-1 sm:flex-none flex flex-col sm:flex-row items-center justify-center py-2.5 sm:py-3 px-3 text-xs sm:text-sm font-semibold transition-all relative ${
            activeTab === 'MAP'
              ? 'text-telecom-navy font-bold after:absolute after:bottom-0 after:left-2 after:right-2 sm:after:left-0 sm:after:right-0 after:h-0.5 after:bg-blue-600'
              : 'text-slate-500 hover:text-slate-800'
          }`}
        >
          <Map className={`w-5 h-5 sm:mr-2 ${activeTab === 'MAP' ? 'text-blue-600' : 'text-slate-400'}`} />
          <span>Sites Map</span>
        </button>

        {/* History Tab with Badges */}
        <button
          onClick={() => setActiveTab('HISTORY')}
          className={`flex-1 sm:flex-none flex flex-col sm:flex-row items-center justify-center py-2.5 sm:py-3 px-3 text-xs sm:text-sm font-semibold transition-all relative ${
            activeTab === 'HISTORY'
              ? 'text-telecom-navy font-bold after:absolute after:bottom-0 after:left-2 after:right-2 sm:after:left-0 sm:after:right-0 after:h-0.5 after:bg-blue-600'
              : 'text-slate-500 hover:text-slate-800'
          }`}
        >
          <div className="relative">
            <History className={`w-5 h-5 sm:mr-2 ${activeTab === 'HISTORY' ? 'text-blue-600' : 'text-slate-400'}`} />
            {pendingCount > 0 ? (
              <span className="absolute -top-1 -right-1 sm:right-1 bg-amber-500 text-white rounded-full text-[10px] w-4 h-4 flex items-center justify-center font-bold shadow-sm">
                {pendingCount}
              </span>
            ) : reports.length > 0 ? (
              <span className="absolute -top-1 -right-1 sm:right-1 bg-slate-300 text-slate-700 rounded-full text-[10px] px-1 h-4 min-w-4 flex items-center justify-center font-bold">
                {reports.length}
              </span>
            ) : null}
          </div>
          <span>History</span>
        </button>

        {/* Settings Tab (Admin only) */}
        {isAdmin && (
          <button
            onClick={() => setActiveTab('SETTINGS')}
            className={`flex-1 sm:flex-none flex flex-col sm:flex-row items-center justify-center py-2.5 sm:py-3 px-3 text-xs sm:text-sm font-semibold transition-all relative ${
              activeTab === 'SETTINGS'
                ? 'text-telecom-navy font-bold after:absolute after:bottom-0 after:left-2 after:right-2 sm:after:left-0 sm:after:right-0 after:h-0.5 after:bg-blue-600'
                : 'text-slate-500 hover:text-slate-800'
            }`}
          >
            <Settings className={`w-5 h-5 sm:mr-2 ${activeTab === 'SETTINGS' ? 'text-blue-600' : 'text-slate-400'}`} />
            <span>Settings</span>
          </button>
        )}
      </div>
    </nav>
  );
};
