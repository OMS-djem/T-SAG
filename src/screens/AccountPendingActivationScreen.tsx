import React, { useState } from 'react';
import { useApp } from '../context/AppContext';
import { StorageService } from '../services/storage';
import { Hourglass, ShieldAlert, RefreshCw, LogOut, RadioTower } from 'lucide-react';

export const AccountPendingActivationScreen: React.FC = () => {
  const { currentUser, signOut, signIn } = useApp();
  const [isChecking, setIsChecking] = useState(false);
  const [statusNote, setStatusNote] = useState<string | null>(null);

  const checkStatus = async () => {
    if (!currentUser) return;
    setIsChecking(true);
    setStatusNote(null);
    await new Promise(r => setTimeout(r, 800));

    // Reload from storage
    const users = StorageService.getUsers();
    const fresh = users.find(u => u.id === currentUser.id);

    setIsChecking(false);
    if (fresh?.isActive) {
      await signIn(fresh.email);
    } else {
      setStatusNote('Your account is still awaiting approval by a Mobiserve Holding Administrator.');
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-b from-telecom-navy via-slate-900 to-slate-950 text-white flex flex-col justify-center items-center px-4 py-8">
      <div className="max-w-md w-full bg-white rounded-2xl shadow-2xl p-6 text-slate-800 text-center space-y-5">
        <div className="w-16 h-16 rounded-full bg-amber-100 text-amber-600 flex items-center justify-center mx-auto shadow-inner">
          <Hourglass className="w-8 h-8 animate-pulse" />
        </div>

        <div className="space-y-1">
          <span className="text-[10px] font-black uppercase tracking-wider text-amber-600 bg-amber-50 px-2 py-0.5 rounded-full border border-amber-200">
            Account Pending Activation
          </span>
          <h2 className="text-xl font-black text-slate-900">Security Clearance Required</h2>
          <p className="text-xs text-slate-500 max-w-sm mx-auto leading-relaxed">
            Welcome to Mobiserve T-SAG, <strong className="text-slate-800">{currentUser?.name}</strong>. Your account has been registered with requested role <strong className="text-blue-600">{currentUser?.role}</strong>.
          </p>
        </div>

        <div className="bg-slate-50 border border-slate-200 rounded-xl p-4 text-xs text-left space-y-1.5 font-mono">
          <div className="flex justify-between">
            <span className="text-slate-500">Email:</span>
            <span className="text-slate-800 font-bold">{currentUser?.email}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-slate-500">Department:</span>
            <span className="text-slate-800">{currentUser?.department}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-slate-500">Region:</span>
            <span className="text-slate-800">{currentUser?.region}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-slate-500">Status:</span>
            <span className="text-amber-600 font-bold">AWAITING_ADMIN_ACTIVATION</span>
          </div>
        </div>

        {statusNote && (
          <p className="text-xs text-amber-700 bg-amber-50 p-2.5 rounded-lg border border-amber-200 font-medium">
            {statusNote}
          </p>
        )}

        <div className="space-y-2 pt-2">
          <button
            type="button"
            disabled={isChecking}
            onClick={checkStatus}
            className="w-full py-2.5 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-bold text-xs shadow-md transition-colors flex items-center justify-center disabled:opacity-50"
          >
            <RefreshCw className={`w-4 h-4 mr-1.5 ${isChecking ? 'animate-spin' : ''}`} />
            Check Activation Status
          </button>

          <button
            type="button"
            onClick={signOut}
            className="w-full py-2.5 rounded-xl border border-slate-200 hover:bg-slate-50 text-slate-600 font-bold text-xs transition-colors flex items-center justify-center"
          >
            <LogOut className="w-4 h-4 mr-1.5" /> Sign Out Session
          </button>
        </div>
      </div>
    </div>
  );
};
