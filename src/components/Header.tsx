import React, { useState } from 'react';
import { useApp } from '../context/AppContext';
import { USER_ROLES } from '../types';
import { RadioTower, LogOut, Wifi, WifiOff } from 'lucide-react';

export const Header: React.FC = () => {
  const { currentUser, currentRole, signOut, activeTab, isOnline, toastMessage, clearToast } = useApp();
  const [showSignOutModal, setShowSignOutModal] = useState(false);

  const roleInfo = USER_ROLES[currentRole] || USER_ROLES.TECHNICIAN;

  const tabTitles = {
    AUDIT: 'Site Audit',
    MAP: 'Sites Map & Radar',
    HISTORY: 'Audit Logs',
    SETTINGS: 'Settings & Administration'
  };

  return (
    <>
      <header className="bg-telecom-navy text-white sticky top-0 z-30 shadow-md">
        <div className="max-w-5xl mx-auto px-4 py-3 flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 rounded-xl bg-blue-600/30 border border-blue-400/40 flex items-center justify-center text-sky-400 shadow-inner">
              <RadioTower className="w-6 h-6" />
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <span className="text-xs uppercase font-extrabold tracking-wider text-sky-400">Mobiserve T-SAG</span>
                <span 
                  className="px-2 py-0.5 rounded text-[10px] font-black uppercase text-white shadow-sm"
                  style={{ backgroundColor: roleInfo.color }}
                  title={roleInfo.description}
                >
                  {roleInfo.code}
                </span>
                <div className="flex items-center space-x-1 pl-1">
                  {isOnline ? (
                    <span className="flex items-center text-[10px] text-emerald-400 font-medium">
                      <Wifi className="w-3 h-3 mr-0.5" /> Online
                    </span>
                  ) : (
                    <span className="flex items-center text-[10px] text-amber-400 font-medium animate-pulse">
                      <WifiOff className="w-3 h-3 mr-0.5" /> Offline
                    </span>
                  )}
                </div>
              </div>
              <h1 className="text-base font-bold text-white leading-tight">
                {tabTitles[activeTab]}
              </h1>
            </div>
          </div>

          <div className="flex items-center space-x-2">
            {currentUser && (
              <div className="hidden sm:flex flex-col text-right mr-1">
                <span className="text-xs font-semibold text-slate-200">{currentUser.name}</span>
                <span className="text-[10px] text-slate-400 truncate max-w-[140px]">{currentUser.email}</span>
              </div>
            )}
            <button
              onClick={() => setShowSignOutModal(true)}
              className="p-2 rounded-lg bg-white/10 hover:bg-white/20 active:bg-white/30 text-white transition-colors"
              title="Sign Out Session"
            >
              <LogOut className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Global Toast Notification */}
        {toastMessage && (
          <div className="bg-sky-600 text-white text-xs px-4 py-2 flex items-center justify-between shadow transition-all">
            <span className="font-medium truncate">{toastMessage}</span>
            <button 
              onClick={clearToast}
              className="ml-2 text-white/80 hover:text-white text-xs font-bold px-1"
            >
              ✕
            </button>
          </div>
        )}
      </header>

      {/* Sign Out Confirmation Modal */}
      {showSignOutModal && (
        <div className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-sm w-full p-6 shadow-2xl text-slate-800 animate-in fade-in zoom-in-95">
            <h3 className="text-lg font-bold text-slate-900 mb-2">Sign Out Session</h3>
            <p className="text-sm text-slate-600 mb-6">
              Are you sure you want to sign out of Mobiserve T-SAG as <span className="font-semibold text-slate-800">{currentUser?.email}</span>?
            </p>
            <div className="flex justify-end space-x-3">
              <button
                type="button"
                onClick={() => setShowSignOutModal(false)}
                className="px-4 py-2 text-sm font-medium text-slate-600 hover:bg-slate-100 rounded-xl transition-colors"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={() => {
                  setShowSignOutModal(false);
                  signOut();
                }}
                className="px-4 py-2 text-sm font-medium bg-red-600 hover:bg-red-700 text-white rounded-xl shadow transition-colors"
              >
                Sign Out
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
};
